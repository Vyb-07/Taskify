package com.taskify.taskify.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Role;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.IdempotencyKeyRepository;
import com.taskify.taskify.repository.RefreshTokenRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskIdempotencyIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private TaskRepository taskRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private IdempotencyKeyRepository idempotencyKeyRepository;

        @Autowired
        private org.springframework.cache.CacheManager cacheManager;

        private User user;

        @BeforeEach
        void setUp() {
                taskRepository.deleteAll();
                idempotencyKeyRepository.deleteAll();
                refreshTokenRepository.deleteAll();
                userRepository.deleteAll();

                // Clear caches
                cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

                Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                                .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_USER)));

                user = new User("testuser", "test@example.com", "password");
                user.setRoles(Set.of(userRole));
                user = userRepository.save(user);
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        void shouldCreateTaskAndStoreResponseOnFirstRequest() throws Exception {
                TaskRequest request = new TaskRequest();
                request.setTitle("Idempotent Task");
                request.setDescription("Desc");
                request.setStatus(Status.PENDING);
                request.setPriority(Priority.MEDIUM);

                String idempotencyKey = UUID.randomUUID().toString();

                mockMvc.perform(post("/api/v1/tasks")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title", is("Idempotent Task")));

                assertEquals(1, taskRepository.count());
                assertEquals(1, idempotencyKeyRepository.count());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        void shouldReturnStoredResponseOnDuplicateRequest() throws Exception {
                TaskRequest request = new TaskRequest();
                request.setTitle("Idempotent Task");
                request.setDescription("Desc");
                request.setStatus(Status.PENDING);
                request.setPriority(Priority.MEDIUM);

                String idempotencyKey = UUID.randomUUID().toString();

                // First request
                MvcResult firstResult = mockMvc.perform(post("/api/v1/tasks")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String firstResponse = firstResult.getResponse().getContentAsString();

                // Second request
                MvcResult secondResult = mockMvc.perform(post("/api/v1/tasks")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String secondResponse = secondResult.getResponse().getContentAsString();

                assertEquals(firstResponse, secondResponse);
                assertEquals(1, taskRepository.count(), "Should not create a second task");
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        void shouldRejectMismatchedPayloadWithSameKey() throws Exception {
                TaskRequest request1 = new TaskRequest();
                request1.setTitle("Task 1");
                request1.setDescription("Desc");
                request1.setStatus(Status.PENDING);
                request1.setPriority(Priority.MEDIUM);

                TaskRequest request2 = new TaskRequest();
                request2.setTitle("Task 2");
                request2.setDescription("Desc");
                request2.setStatus(Status.PENDING);
                request2.setPriority(Priority.MEDIUM);

                String idempotencyKey = UUID.randomUUID().toString();

                // First request
                mockMvc.perform(post("/api/v1/tasks")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request1)))
                                .andExpect(status().isCreated());

                // Second request with different payload
                mockMvc.perform(post("/api/v1/tasks")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2)))
                                .andExpect(status().isConflict()); // Or BadRequest depending on implementation, but
                                                                   // implementation used
                                                                   // Conflict via IdempotencyException

                assertEquals(1, taskRepository.count());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        void shouldCreateSeparateTasksWithDifferentKeys() throws Exception {
                TaskRequest request = new TaskRequest();
                request.setTitle("Shared Payload");
                request.setDescription("Desc");
                request.setStatus(Status.PENDING);
                request.setPriority(Priority.MEDIUM);

                mockMvc.perform(post("/api/v1/tasks")
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/v1/tasks")
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                assertEquals(2, taskRepository.count());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        void shouldCreateTaskNormallyWhenKeyIsMissing() throws Exception {
                TaskRequest request = new TaskRequest();
                request.setTitle("No Key");
                request.setDescription("Desc");
                request.setStatus(Status.PENDING);
                request.setPriority(Priority.MEDIUM);

                mockMvc.perform(post("/api/v1/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/v1/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                assertEquals(2, taskRepository.count());
        }
}
