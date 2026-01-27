package com.taskify.taskify.controller;

import com.taskify.taskify.model.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DeprecationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername("testuser").isEmpty()) {
            User user = new User();
            user.setUsername("testuser");
            user.setEmail("test@example.com");
            user.setPassword("password");
            userRepository.save(user);
        }
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    public void deprecatedEndpointReturnsDeprecationHeaders() throws Exception {
        // We marked getTaskById as deprecated. Endpoint: /api/v1/tasks/{id}
        // Even if task doesn't exist, the interceptor runs before the controller logic
        mockMvc.perform(get("/api/v1/tasks/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().exists("Sunset"))
                .andExpect(header().string("Link", org.hamcrest.Matchers.containsString("rel=\"successor-version\"")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    public void nonDeprecatedEndpointDoesNotHaveDeprecationHeaders() throws Exception {
        // getTasks is not deprecated
        mockMvc.perform(get("/api/v1/tasks")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));
    }
}
