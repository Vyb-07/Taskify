package com.taskify.taskify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.TaskRepository;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskRationaleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("rationaleUser").orElseGet(() -> {
            User user = new User();
            user.setUsername("rationaleUser");
            user.setEmail("rationale@example.com");
            user.setPassword("password");
            return userRepository.save(user);
        });
        taskRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "rationaleUser")
    void shouldCreateTaskWithRationale() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Task with rationale");
        request.setDescription("Desc");
        request.setRationale("The why");
        request.setStatus(Status.PENDING);
        request.setPriority(Priority.MEDIUM);
        request.setDueDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rationale").value("The why"));
    }

    @Test
    @WithMockUser(username = "rationaleUser")
    void shouldUpdateTaskRationale() throws Exception {
        // First create a task
        TaskRequest createRequest = new TaskRequest();
        createRequest.setTitle("Initial title");
        createRequest.setRationale("Initial rationale");
        createRequest.setStatus(Status.PENDING);
        createRequest.setDueDate(LocalDateTime.now().plusDays(1));

        String response = mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(response).get("id").asLong();

        // Now update it
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTitle("Updated title");
        updateRequest.setRationale("Updated rationale");
        updateRequest.setStatus(Status.IN_PROGRESS);
        updateRequest.setDueDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rationale").value("Updated rationale"));
    }

    @Test
    @WithMockUser(username = "rationaleUser")
    void shouldHandleNullRationale() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Task without rationale");
        request.setStatus(Status.PENDING);
        request.setDueDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rationale").value(nullValue()));
    }
}
