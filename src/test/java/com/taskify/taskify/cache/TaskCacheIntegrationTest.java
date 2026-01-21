package com.taskify.taskify.cache;

import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.dto.TaskResponse;
import com.taskify.taskify.model.*;
import com.taskify.taskify.repository.RoleRepository;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.security.SecurityConstants;
import com.taskify.taskify.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TaskCacheIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Clear caches
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

        Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_USER)));

        Role adminRole = roleRepository.findByName(SecurityConstants.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_ADMIN)));

        user1 = new User("user1", "user1@example.com", "password");
        user1.setRoles(Set.of(userRole));
        user1 = userRepository.save(user1);

        user2 = new User("user2", "user2@example.com", "password");
        user2.setRoles(Set.of(userRole));
        user2 = userRepository.save(user2);

        User adminUser = new User("adminuser", "admin@example.com", "password");
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void shouldCacheAndInvalidateTaskDetails() {
        Task task = new Task("Task 1", "Desc", Status.PENDING, Priority.MEDIUM, LocalDateTime.now().plusDays(1), user1);
        task = taskRepository.save(task);
        Long taskId = task.getId();

        // First call - should populate cache
        TaskResponse response1 = taskService.getTaskById(taskId);
        Cache detailCache = cacheManager.getCache("taskDetails");
        assertNotNull(detailCache.get(taskId));

        // Update task - should invalidate cache
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTitle("Updated Task");
        updateRequest.setStatus(Status.IN_PROGRESS);
        taskService.updateTask(taskId, updateRequest);

        assertNull(detailCache.get(taskId));

        // Second call - should re-populate
        taskService.getTaskById(taskId);
        assertNotNull(detailCache.get(taskId));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void shouldCacheAndInvalidateTaskLists() {
        taskService.getAllTasks(null, null, null, null, null, null, null, false, PageRequest.of(0, 10));

        Cache versionCache = cacheManager.getCache("taskVersions");
        String versionBefore = versionCache.get("user1", String.class);
        assertNotNull(versionBefore);

        // Create a task - should increment version
        TaskRequest createRequest = new TaskRequest();
        createRequest.setTitle("New Task");
        createRequest.setStatus(Status.PENDING);
        createRequest.setPriority(Priority.MEDIUM);
        taskService.createTask(createRequest);

        String versionAfter = versionCache.get("user1", String.class);
        assertNotEquals(versionBefore, versionAfter);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void shouldMaintainUserIsolationInCache() {
        // user1 populates list cache
        taskService.getAllTasks(null, null, null, null, null, null, null, false, PageRequest.of(0, 10));

        Cache versionCache = cacheManager.getCache("taskVersions");
        String user1VersionBefore = versionCache.get("user1", String.class);
        assertNotNull(user1VersionBefore);

        // We can't use @WithMockUser twice in one method easily,
        // but we can verify that user2 version is null initially
        assertNull(versionCache.get("user2", String.class));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void adminShouldHaveSeparateCacheKey() {
        // Just verify it populates for admin too
        taskService.getAllTasks(null, null, null, null, null, null, null, false, PageRequest.of(0, 10));
        Cache versionCache = cacheManager.getCache("taskVersions");
        assertNotNull(versionCache.get("adminuser", String.class));
    }
}
