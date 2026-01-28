package com.taskify.taskify.controller;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class StagnantTasksIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("stagnantUser").orElseGet(() -> {
            User user = new User();
            user.setUsername("stagnantUser");
            user.setEmail("stagnant@example.com");
            user.setPassword("password");
            Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_USER)));
            user.setRoles(Set.of(userRole));
            return userRepository.save(user);
        });

        taskRepository.deleteAll();
    }

    private void forceLastModified(long taskId, LocalDateTime time) {
        jdbcTemplate.update("UPDATE tasks SET last_modified_at = ? WHERE id = ?", time, taskId);
    }

    @Test
    @WithMockUser(username = "stagnantUser", roles = "USER")
    public void stagnantTasksDetectsOverdueInactiveWork() throws Exception {
        // 1. Overdue (>2d) and Inactive
        Task t1 = createTask("Overdue Stagnant", Status.PENDING, Priority.MEDIUM, LocalDateTime.now().minusDays(5));
        forceLastModified(t1.getId(), LocalDateTime.now().minusDays(3));

        // 2. Overdue but recently updated (Excluded)
        Task t2 = createTask("Overdue Active", Status.PENDING, Priority.MEDIUM, LocalDateTime.now().minusDays(5));
        // lastModifiedAt is now() by default

        // 3. In Progress Stalled (>3d)
        Task t3 = createTask("In Progress Stalled", Status.IN_PROGRESS, Priority.HIGH, LocalDateTime.now().plusDays(5));
        forceLastModified(t3.getId(), LocalDateTime.now().minusDays(4));

        // 4. Pending Forgotten (>7d)
        Task t4 = createTask("Pending Forgotten", Status.PENDING, Priority.LOW, LocalDateTime.now().plusDays(10));
        forceLastModified(t4.getId(), LocalDateTime.now().minusDays(8));

        // 5. Completed (Excluded even if old)
        Task t5 = createTask("Old Completed", Status.COMPLETED, Priority.HIGH, LocalDateTime.now().minusDays(5));
        forceLastModified(t5.getId(), LocalDateTime.now().minusDays(10));

        mockMvc.perform(get("/api/v1/tasks/stagnant")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Pending Forgotten")) // Oldest lastModifiedAt (8d ago)
                .andExpect(jsonPath("$[1].title").value("In Progress Stalled")) // (4d ago)
                .andExpect(jsonPath("$[2].title").value("Overdue Stagnant")); // (3d ago)
    }

    @Test
    @WithMockUser(username = "otherUser", roles = "USER")
    public void stagnantTasksRespectsOwnership() throws Exception {
        userRepository.save(new User("otherUser", "other@example.com", "password"));

        // Task for stagnantUser
        Task t1 = createTask("Stagnant Task", Status.PENDING, Priority.MEDIUM, LocalDateTime.now().minusDays(5));
        forceLastModified(t1.getId(), LocalDateTime.now().minusDays(10));

        mockMvc.perform(get("/api/v1/tasks/stagnant")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private Task createTask(String title, Status status, Priority priority, LocalDateTime dueDate) {
        Task task = new Task(title, "Description", status, priority, dueDate, testUser);
        return taskRepository.save(task);
    }
}
