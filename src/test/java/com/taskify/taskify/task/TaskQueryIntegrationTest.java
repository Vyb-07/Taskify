package com.taskify.taskify.task;

import com.taskify.taskify.model.*;
import com.taskify.taskify.repository.RoleRepository;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.security.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_USER)));
        Role adminRole = roleRepository.findByName(SecurityConstants.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_ADMIN)));

        user = new User("testuser", "test@example.com", "password");
        user.setRoles(Set.of(userRole));
        user = userRepository.save(user);

        admin = new User("adminuser", "admin@example.com", "password");
        admin.setRoles(Set.of(adminRole));
        admin = userRepository.save(admin);

        // Create some tasks
        taskRepository.save(
                new Task("Task 1", "Description 1", Status.PENDING, Priority.HIGH, LocalDateTime.now().plusDays(1),
                        user));
        taskRepository.save(new Task("Task 2", "Search Me", Status.IN_PROGRESS, Priority.MEDIUM,
                LocalDateTime.now().plusDays(2), user));
        taskRepository
                .save(new Task("Task 3", "Other", Status.COMPLETED, Priority.LOW, LocalDateTime.now().plusDays(3),
                        user));

        Task deletedTask = new Task("Deleted Task", "Hidden", Status.PENDING, Priority.MEDIUM,
                LocalDateTime.now().plusDays(1), user);
        deletedTask.setDeleted(true);
        taskRepository.save(deletedTask);

        taskRepository.save(
                new Task("Admin Task", "Secret", Status.PENDING, Priority.HIGH, LocalDateTime.now().plusDays(1),
                        admin));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/tasks?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldFilterByPriority() throws Exception {
        mockMvc.perform(get("/api/tasks?priority=HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldSearchByKeyword() throws Exception {
        mockMvc.perform(get("/api/tasks?keyword=Search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Task 2"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldNotSeeDeletedTasksByDefault() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3))) // 1, 2, 3 (Admin Task hidden by ownership)
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Deleted Task"))));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldNotSeeOtherUsersTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Admin Task"))));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void adminShouldSeeAllTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4)); // 1, 2, 3, Admin Task (Deleted still hidden by
                                                                  // default)
    }

    @Test
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void adminShouldSeeDeletedTasksWhenRequested() throws Exception {
        mockMvc.perform(get("/api/tasks?includeDeleted=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem("Deleted Task")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void userShouldGetForbiddenForIncludeDeleted() throws Exception {
        mockMvc.perform(get("/api/tasks?includeDeleted=true"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldSortByPriorityDesc() throws Exception {
        mockMvc.perform(get("/api/tasks?sortBy=priority&direction=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].priority").value("MEDIUM")) // MEDIUM > LOW in string sort? Wait.
                // Enums are usually sorted by ordinal or name. Priority: LOW, MEDIUM, HIGH.
                // HIGH > MEDIUM > LOW alphabetically? H > M > L. Yes.
                .andExpect(jsonPath("$.content[0].title").value("Task 2")); // MEDIUM
    }
}
