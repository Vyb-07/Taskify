package com.taskify.taskify.controller;

import com.taskify.taskify.model.*;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FocusModeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("focusUser").orElseGet(() -> {
            User user = new User();
            user.setUsername("focusUser");
            user.setEmail("focus@example.com");
            user.setPassword("password");
            Role userRole = roleRepository.findByName(SecurityConstants.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_USER)));
            user.setRoles(Set.of(userRole));
            return userRepository.save(user);
        });

        taskRepository.deleteAll();

        // 1. Overdue task (Priority: Highest urgency)
        createTask("Overdue Task", Status.PENDING, Priority.MEDIUM, LocalDateTime.now().minusDays(1), testUser);

        // 2. High Priority task due soon
        createTask("High Priority Soon", Status.IN_PROGRESS, Priority.HIGH, LocalDateTime.now().plusHours(2), testUser);

        // 3. Medium Priority task due tomorrow
        createTask("Medium Soon", Status.PENDING, Priority.MEDIUM, LocalDateTime.now().plusDays(1), testUser);

        // 4. Low Priority but overdue
        createTask("Low Overdue", Status.PENDING, Priority.LOW, LocalDateTime.now().minusHours(5), testUser);

        // 5. High Priority but far future
        createTask("High Future", Status.PENDING, Priority.HIGH, LocalDateTime.now().plusDays(10), testUser);

        // 6. Completed task (Excluded)
        createTask("Completed Task", Status.COMPLETED, Priority.HIGH, LocalDateTime.now().minusDays(1), testUser);

        // 7. Deleted task (Excluded)
        Task deletedTask = createTask("Deleted Task", Status.PENDING, Priority.HIGH, LocalDateTime.now().plusHours(1),
                testUser);
        deletedTask.setDeleted(true);
        taskRepository.save(deletedTask);
    }

    private Task createTask(String title, Status status, Priority priority, LocalDateTime dueDate, User owner) {
        Task task = new Task(title, "Description", status, priority, dueDate, owner);
        return taskRepository.save(task);
    }

    @Test
    @WithMockUser(username = "focusUser", roles = "USER")
    public void focusModeReturnsTop5UrgentAndPrioritizedTasks() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/focus")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].title").value("Overdue Task"))
                .andExpect(jsonPath("$[1].title").value("Low Overdue"))
                .andExpect(jsonPath("$[2].title").value("High Priority Soon"))
                .andExpect(jsonPath("$[3].title").value("Medium Soon"))
                .andExpect(jsonPath("$[4].title").value("High Future"));
    }

    @Test
    @WithMockUser(username = "otherUser", roles = "USER")
    public void focusModeRespectsOwnership() throws Exception {
        userRepository.save(new User("otherUser", "other@example.com", "password"));

        mockMvc.perform(get("/api/v1/tasks/focus")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
