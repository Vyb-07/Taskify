package com.taskify.taskify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.dto.LoginRequest;
import com.taskify.taskify.dto.RegisterRequest;
import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.model.Role;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.repository.RefreshTokenRepository;
import com.taskify.taskify.repository.RoleRepository;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private RateLimitService rateLimitService;

        @Autowired
        private TaskRepository taskRepository;

        private String jwtToken;

        @BeforeEach
        void setUp() throws Exception {
                rateLimitService.clearBuckets();
                refreshTokenRepository.deleteAll();
                taskRepository.deleteAll();
                userRepository.deleteAll();
                roleRepository.findByName("ROLE_USER")
                                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

                // Register and login to get JWT
                RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "password");
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated());

                LoginRequest loginRequest = new LoginRequest("testuser", "password");
                MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                Map<String, String> responseMap = objectMapper.readValue(responseBody,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                                });
                jwtToken = "Bearer " + responseMap.get("token");
        }

        @Test
        void shouldCreateTaskSuccessfully() throws Exception {
                TaskRequest request = new TaskRequest();
                request.setTitle("Integration Test Task");
                request.setDescription("Details");
                request.setStatus(Status.PENDING);
                request.setDueDate(LocalDateTime.now().plusDays(1));

                mockMvc.perform(post("/api/v1/tasks")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("Integration Test Task"));
        }

        @Test
        void shouldReturn401WhenJwtIsMissing() throws Exception {
                mockMvc.perform(get("/api/v1/tasks"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldGetAllTasks() throws Exception {
                mockMvc.perform(get("/api/v1/tasks")
                                .header("Authorization", jwtToken))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldGetTaskById() throws Exception {
                // First create a task
                TaskRequest request = new TaskRequest();
                request.setTitle("Task 1");
                request.setStatus(Status.PENDING);

                MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                Map<String, Object> taskMap = objectMapper.readValue(responseBody,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                });
                Integer id = (Integer) taskMap.get("id");

                mockMvc.perform(get("/api/v1/tasks/" + id)
                                .header("Authorization", jwtToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Task 1"));
        }
}
