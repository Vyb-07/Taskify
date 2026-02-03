package com.taskify.taskify.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // Create the user that WithMockUser expects
        userRepository.save(new com.taskify.taskify.model.User("testuser", "test@example.com", "password"));
    }

    @Test
    void shouldReturn401JsonForUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldReturn400ForValidationFailure() throws Exception {
        TaskRequest invalidRequest = new TaskRequest();
        invalidRequest.setTitle(""); // Invalid - @NotBlank

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value(containsString("title")))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldReturn404ForNonExistentTask() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldReturn403ForForbiddenAccess() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldIncludeCorrelationIdInErrorResponse() throws Exception {
        String correlationId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/tasks/999999")
                .header("X-Correlation-ID", correlationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.correlationId").value(correlationId));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldReturn409ForIdempotencyConflict() throws Exception {
        String key = "test-key-conflict";
        TaskRequest req1 = new TaskRequest();
        req1.setTitle("Task 1");
        req1.setStatus(Status.PENDING);
        req1.setPriority(Priority.MEDIUM);

        TaskRequest req2 = new TaskRequest();
        req2.setTitle("Task 2 (Same Key)");
        req2.setStatus(Status.PENDING);
        req2.setPriority(Priority.MEDIUM);

        // First request creates the task and saves the key
        mockMvc.perform(post("/api/v1/tasks")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Second request with same key but different payload causes 409 Conflict
        mockMvc.perform(post("/api/v1/tasks")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Idempotency Error"))
                .andExpect(jsonPath("$.correlationId").exists());
    }
}
