package com.taskify.taskify.controller;

import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.Task;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaskExplainabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setEmail("test@example.com");
        testUser.setRoles(Collections.emptySet());
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldIncludeExplanationsInFocusMode() throws Exception {
        createTask("High Priority Overdue", LocalDateTime.now().minusDays(1), Priority.HIGH);

        mockMvc.perform(get("/api/v1/tasks/focus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].explanation", is("Overdue by 1 days and high priority")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldIncludeExplanationsInStagnantTasks() throws Exception {
        Task task = createTask("Stagnant Task", null, Priority.MEDIUM);

        // Bypass JPA hooks to set lastModifiedAt in the past
        jdbcTemplate.update("UPDATE tasks SET last_modified_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(10)), task.getId());

        mockMvc.perform(get("/api/v1/tasks/stagnant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].explanation", containsString("Pending for 10 days")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldNotIncludeExplanationsInGenericList() throws Exception {
        createTask("Normal Task", null, Priority.MEDIUM);

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].explanation").doesNotExist());
    }

    private Task createTask(String title, LocalDateTime dueDate, Priority priority) {
        Task task = new Task();
        task.setTitle(title);
        task.setOwner(testUser);
        task.setStatus(Status.PENDING);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        return taskRepository.save(task);
    }
}
