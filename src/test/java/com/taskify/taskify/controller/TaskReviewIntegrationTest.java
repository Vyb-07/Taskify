package com.taskify.taskify.controller;

import com.taskify.taskify.dto.TaskReviewResponse;
import com.taskify.taskify.model.AuditAction;
import com.taskify.taskify.model.AuditLog;
import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.Task;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.AuditLogRepository;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
public class TaskReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("reviewer");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setEmail("reviewer@example.com");
        testUser.setRoles(Collections.emptySet());
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "reviewer")
    void shouldGenerateEmptyReviewWhenNoDataExists() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("last_7_days"))
                .andExpect(jsonPath("$.summary.createdTasks").value(0))
                .andExpect(jsonPath("$.summary.completedTasks").value(0))
                .andExpect(
                        jsonPath("$.insights", hasItem("Your task patterns are stable. Keep up the consistent work.")));
    }

    @Test
    @WithMockUser(username = "reviewer")
    void shouldAggregateStatsCorrectly() throws Exception {
        // Create 2 tasks in last 7 days
        createTask("Task 1", LocalDateTime.now().minusDays(1));
        createTask("Task 2", LocalDateTime.now().minusDays(2));

        // Mock 1 completion in audit logs
        logAuditAction(AuditAction.TASK_UPDATE, LocalDateTime.now().minusDays(1));

        // Mock 1 focus mode usage
        logAuditAction(AuditAction.FOCUS_MODE_USAGE, LocalDateTime.now().minusDays(1));

        mockMvc.perform(get("/api/v1/tasks/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.createdTasks").value(2))
                .andExpect(jsonPath("$.summary.completedTasks").value(1))
                .andExpect(jsonPath("$.summary.focusModeUsages").value(1));
    }

    @Test
    @WithMockUser(username = "reviewer")
    void shouldGenerateVelocityInsightWhenCompletingMoreTasks() throws Exception {
        // Create 1 task
        createTask("New Task", LocalDateTime.now().minusDays(2));

        // Mock 3 completions
        logAuditAction(AuditAction.TASK_UPDATE, LocalDateTime.now().minusDays(1));
        logAuditAction(AuditAction.TASK_UPDATE, LocalDateTime.now().minusDays(2));
        logAuditAction(AuditAction.TASK_UPDATE, LocalDateTime.now().minusDays(3));

        mockMvc.perform(get("/api/v1/tasks/review"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.insights", hasItem("Great velocity! You're clearing your backlog effectively.")));
    }

    private void createTask(String title, LocalDateTime createdAt) {
        Task task = new Task();
        task.setTitle(title);
        task.setOwner(testUser);
        task.setStatus(Status.PENDING);
        task.setPriority(Priority.MEDIUM);
        task.setCreatedAt(createdAt);
        task.setLastModifiedAt(createdAt);
        taskRepository.save(task);
    }

    private void logAuditAction(AuditAction action, LocalDateTime timestamp) {
        AuditLog log = new AuditLog();
        log.setActorUserId(testUser.getId());
        log.setAction(action);
        log.setTimestamp(timestamp);
        auditLogRepository.save(log);
    }
}
