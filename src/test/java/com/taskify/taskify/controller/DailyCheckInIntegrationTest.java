package com.taskify.taskify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.dto.DailyCheckInRequest;
import com.taskify.taskify.model.*;
import com.taskify.taskify.repository.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DailyCheckInIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DailyIntentRepository dailyIntentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("checkinUser").orElseGet(() -> {
            User user = new User();
            user.setUsername("checkinUser");
            user.setEmail("checkin@example.com");
            user.setPassword("password");
            return userRepository.save(user);
        });

        taskRepository.deleteAll();
        dailyIntentRepository.deleteAll();

        task1 = createTask("Task 1", Status.PENDING, testUser);
        task2 = createTask("Task 2", Status.IN_PROGRESS, testUser);
    }

    private Task createTask(String title, Status status, User owner) {
        Task task = new Task(title, "Desc", status, Priority.MEDIUM, LocalDateTime.now().plusDays(1), owner);
        return taskRepository.save(task);
    }

    @Test
    @WithMockUser(username = "checkinUser")
    void shouldCreateDailyCheckIn() throws Exception {
        DailyCheckInRequest request = new DailyCheckInRequest();
        request.setTaskIds(List.of(task1.getId()));
        request.setNote("Today's focus");

        mockMvc.perform(post("/api/v1/day/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("Today's focus"))
                .andExpect(jsonPath("$.todayTasks", hasSize(1)))
                .andExpect(jsonPath("$.todayTasks[0].id").value(task1.getId()));
    }

    @Test
    @WithMockUser(username = "checkinUser")
    void shouldReturnEmptyIntentWhenMissingButStillShowSuggestions() throws Exception {
        mockMvc.perform(get("/api/v1/day/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").isEmpty())
                .andExpect(jsonPath("$.todayTasks", hasSize(0)))
                .andExpect(jsonPath("$.suggestedTasks", not(empty())));
    }

    @Test
    @WithMockUser(username = "checkinUser")
    void shouldCarryOverUnfinishedTasksFromYesterday() throws Exception {
        // Create yesterday's intent
        DailyIntent yesterdayIntent = new DailyIntent();
        yesterdayIntent.setUserId(testUser.getId());
        yesterdayIntent.setDate(LocalDate.now().minusDays(1));
        yesterdayIntent.setTaskIds(List.of(task1.getId(), task2.getId()));
        dailyIntentRepository.save(yesterdayIntent);

        // Mark task 2 as completed
        task2.setStatus(Status.COMPLETED);
        taskRepository.save(task2);

        mockMvc.perform(get("/api/v1/day/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carryoverTasks", hasSize(1)))
                .andExpect(jsonPath("$.carryoverTasks[0].id").value(task1.getId()));
    }

    @Test
    @WithMockUser(username = "checkinUser")
    void shouldNotCarryOverTasksFromMoreThanOneDayAgo() throws Exception {
        DailyIntent oldIntent = new DailyIntent();
        oldIntent.setUserId(testUser.getId());
        oldIntent.setDate(LocalDate.now().minusDays(2));
        oldIntent.setTaskIds(List.of(task1.getId()));
        dailyIntentRepository.save(oldIntent);

        mockMvc.perform(get("/api/v1/day/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carryoverTasks", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "checkinUser")
    void shouldEnforceTaskOwnershipOnCheckIn() throws Exception {
        User otherUser = userRepository.save(new User("other", "other@ex.com", "pass"));
        Task otherTask = createTask("Other task", Status.PENDING, otherUser);

        DailyCheckInRequest request = new DailyCheckInRequest();
        request.setTaskIds(List.of(otherTask.getId()));

        mockMvc.perform(post("/api/v1/day/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "checkinUser")
    void shouldValidateTaskCount() throws Exception {
        DailyCheckInRequest request = new DailyCheckInRequest();
        request.setTaskIds(List.of(1L, 2L, 3L, 4L)); // Too many

        mockMvc.perform(post("/api/v1/day/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
