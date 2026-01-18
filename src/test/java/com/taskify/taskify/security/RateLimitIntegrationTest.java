package com.taskify.taskify.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.dto.LoginRequest;
import com.taskify.taskify.dto.RegisterRequest;
import com.taskify.taskify.model.Role;
import com.taskify.taskify.repository.RefreshTokenRepository;
import com.taskify.taskify.repository.RoleRepository;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        rateLimitService.clearBuckets();
    }

    @Test
    void shouldExceedRateLimitForUnauthenticatedRequest() throws Exception {
        // 1. Verify excluded path (55 requests, capacity is 50 but excluded doesn't
        // consume)
        for (int i = 0; i < 55; i++) {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }

        // 2. Test API rate limit (50 requests/min)
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/api/tasks"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 403 && status != 401 && status != 429) {
                            throw new RuntimeException("Unexpected status: " + status);
                        }
                    });
        }

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isTooManyRequests());

        // 3. Test Auth rate limit (10 requests/min) for the same IP
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/auth/login"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 405 && status != 429) {
                            throw new RuntimeException("Unexpected status for auth: " + status);
                        }
                    });
        }
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldExceedRateLimitForAuthenticatedUser() throws Exception {
        // Register and login
        RegisterRequest registerRequest = new RegisterRequest("ratelimituser", "limit@example.com", "password");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("ratelimituser", "password");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, String> responseMap = objectMapper.readValue(loginResult.getResponse().getContentAsString(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                });
        String token = "Bearer " + responseMap.get("token");

        // Authenticated users use the user bucket.
        // API capacity is 50.
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/api/tasks")
                    .header("Authorization", token))
                    .andExpect(status().isOk());
        }

        // The 51st request should be 429
        mockMvc.perform(get("/api/tasks")
                .header("Authorization", token))
                .andExpect(status().isTooManyRequests());

        // Verify that ANOTHER user (or unauthenticated) still has their own limit
        mockMvc.perform(get("/api/tasks")) // Unauthenticated IP
                .andExpect(status().isForbidden()); // Passed rate limit, blocked by security
    }
}
