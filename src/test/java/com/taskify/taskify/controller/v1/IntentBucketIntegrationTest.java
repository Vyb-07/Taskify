package com.taskify.taskify.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.dto.IntentBucketRequest;
import com.taskify.taskify.model.*;
import com.taskify.taskify.repository.IntentBucketRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class IntentBucketIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private IntentBucketRepository intentBucketRepository;

        @Autowired
        private TaskRepository taskRepository;

        @Autowired
        private com.taskify.taskify.repository.RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private org.springframework.cache.CacheManager cacheManager;

        @Autowired
        private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

        private User user1;
        private User user2;

        @BeforeEach
        void setUp() {
                taskRepository.deleteAll();
                intentBucketRepository.deleteAll();
                refreshTokenRepository.deleteAll();
                userRepository.deleteAll();

                // Clear caches to avoid interference
                cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

                Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                                .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_USER)));

                user1 = new User("user1", "user1@example.com", "password");
                user1.setRoles(Set.of(userRole));
                user1 = userRepository.save(user1);

                user2 = new User("user2", "user2@example.com", "password");
                user2.setRoles(Set.of(userRole));
                user2 = userRepository.save(user2);
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void shouldCreateAndListIntents() throws Exception {
                IntentBucketRequest request = new IntentBucketRequest();
                request.setName("Career Growth");
                request.setDescription("Tasks for learning and career advancement");
                request.setColor("#3498db");

                mockMvc.perform(post("/api/v1/intents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Career Growth"))
                                .andExpect(jsonPath("$.color").value("#3498db"));

                mockMvc.perform(get("/api/v1/intents"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].name").value("Career Growth"));
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void shouldEnforceNameUniquenessPerUser() throws Exception {
                IntentBucket intent = new IntentBucket(user1.getId(), "Family", null, null);
                intentBucketRepository.save(intent);

                IntentBucketRequest request = new IntentBucketRequest();
                request.setName("Family");

                mockMvc.perform(post("/api/v1/intents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void shouldMaintainOwnershipIsolation() throws Exception {
                // User 2's intent
                IntentBucket otherIntent = new IntentBucket(user2.getId(), "Secret", null, null);
                intentBucketRepository.save(otherIntent);

                // User 1 should not see it
                mockMvc.perform(get("/api/v1/intents"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void shouldAssociateTaskWithIntent() throws Exception {
                IntentBucket intent = new IntentBucket(user1.getId(), "Health", null, null);
                intent = intentBucketRepository.save(intent);

                com.taskify.taskify.dto.TaskRequest request = new com.taskify.taskify.dto.TaskRequest();
                request.setTitle("Go for a run");
                request.setStatus(Status.PENDING);
                request.setPriority(Priority.MEDIUM);
                request.setIntentBucketId(intent.getId());

                mockMvc.perform(post("/api/v1/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.intentBucketId").value(intent.getId()))
                                .andExpect(jsonPath("$.intentBucketName").value("Health"));
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void shouldClearIntentOnDeletionButKeepTask() throws Exception {
                IntentBucket intent = new IntentBucket(user1.getId(), "Temporary", null, null);
                intent = intentBucketRepository.save(intent);

                Task task = new Task("Persistent Task", "Desc", Status.PENDING, Priority.MEDIUM,
                                LocalDateTime.now().plusDays(1),
                                user1);
                task.setIntentBucket(intent);
                task = taskRepository.save(task);

                mockMvc.perform(delete("/api/v1/intents/" + intent.getId()))
                                .andExpect(status().isNoContent());

                // Verify task still exists but has no intent
                mockMvc.perform(get("/api/v1/tasks/" + task.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Persistent Task"))
                                .andExpect(jsonPath("$.intentBucketId").isEmpty());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void shouldGenerateOverviewWithInsights() throws Exception {
                IntentBucket career = intentBucketRepository
                                .save(new IntentBucket(user1.getId(), "Career", null, null));
                IntentBucket health = intentBucketRepository
                                .save(new IntentBucket(user1.getId(), "Health", null, null));

                // Career: 2 tasks, 1 completed
                taskRepository.save(createTaskWithIntent("Study", Status.COMPLETED, career));
                taskRepository.save(createTaskWithIntent("Interview", Status.IN_PROGRESS, career));

                // Health: 1 task, stagnant (overdue by 5 days)
                Task healthTask = createTaskWithIntent("Gym", Status.PENDING, health);
                healthTask = taskRepository.save(healthTask);

                // Bypass JPA auditing/hooks to set timestamps in the past
                jdbcTemplate.update("UPDATE tasks SET due_date = ?, last_modified_at = ? WHERE id = ?",
                                LocalDateTime.now().minusDays(5),
                                LocalDateTime.now().minusDays(5),
                                healthTask.getId());

                mockMvc.perform(get("/api/v1/intents/overview"))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.insights", hasSize(2)))
                                .andExpect(jsonPath("$.insights[?(@.name=='Career')].totalTasks").value(2))
                                .andExpect(jsonPath("$.insights[?(@.name=='Career')].completedTasks").value(1))
                                .andExpect(jsonPath("$.insights[?(@.name=='Health')].stagnantTasks").value(1));
        }

        private Task createTaskWithIntent(String title, Status status, IntentBucket intent) {
                Task task = new Task(title, "Desc", status, Priority.MEDIUM, LocalDateTime.now().plusDays(1), user1);
                task.setIntentBucket(intent);
                return task;
        }
}
