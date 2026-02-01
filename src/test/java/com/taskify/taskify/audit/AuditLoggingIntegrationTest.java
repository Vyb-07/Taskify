package com.taskify.taskify.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.dto.LoginRequest;
import com.taskify.taskify.dto.RegisterRequest;
import com.taskify.taskify.model.AuditAction;
import com.taskify.taskify.model.AuditLog;
import com.taskify.taskify.model.Role;
import com.taskify.taskify.repository.AuditLogRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        rateLimitService.clearBuckets();
        taskRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        // Clear caches
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
    }

    @Test
    void shouldCreateAuditLogAndCorrelationIdOnRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("audituser", "audit@example.com", "password");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-ID"));

        // Wait a bit for @Async
        Thread.sleep(200);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS); // We used LOGIN_SUCCESS for
                                                                                  // registration too as per my
                                                                                  // implementation
    }

    @Test
    void shouldLogAuditOnLoginFailure() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent", "wrong");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Correlation-ID"));

        Thread.sleep(200);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).anyMatch(log -> log.getAction() == AuditAction.LOGIN_FAILURE);
    }
}
