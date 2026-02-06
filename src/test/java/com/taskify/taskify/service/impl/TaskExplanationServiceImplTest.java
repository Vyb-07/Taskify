package com.taskify.taskify.service.impl;

import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskExplanationServiceImplTest {

    private TaskExplanationServiceImpl explanationService;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        explanationService = new TaskExplanationServiceImpl();
        now = LocalDateTime.of(2026, 2, 6, 12, 0); // Friday
    }

    @Test
    void shouldGenerateFocusExplanationForOverdueTask() {
        Task task = new Task();
        task.setDueDate(now.minusDays(3));
        task.setPriority(Priority.HIGH);

        String explanation = explanationService.generateFocusExplanation(task, now);

        assertEquals("Overdue by 3 days and high priority", explanation);
    }

    @Test
    void shouldGenerateFocusExplanationForDueTodayTask() {
        Task task = new Task();
        task.setDueDate(now);
        task.setPriority(Priority.HIGH);

        String explanation = explanationService.generateFocusExplanation(task, now);

        assertEquals("Due today and high priority", explanation);
    }

    @Test
    void shouldGenerateFocusExplanationForDueSoonTask() {
        Task task = new Task();
        task.setDueDate(now.plusDays(2));
        task.setPriority(Priority.HIGH);

        String explanation = explanationService.generateFocusExplanation(task, now);

        assertEquals("Due soon (in 2 days) and high priority", explanation);
    }

    @Test
    void shouldGenerateStagnantExplanationForInProgressTask() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        task.setLastModifiedAt(now.minusDays(5));

        String explanation = explanationService.generateStagnantExplanation(task, now);

        assertEquals("Inactive for 5 days while still IN_PROGRESS", explanation);
    }

    @Test
    void shouldGenerateStagnantExplanationForPendingTask() {
        Task task = new Task();
        task.setStatus(Status.PENDING);
        task.setLastModifiedAt(now.minusDays(10));

        String explanation = explanationService.generateStagnantExplanation(task, now);

        assertEquals("Pending for 10 days with no recent updates", explanation);
    }

    @Test
    void shouldGenerateStagnantExplanationForOverdueStagnantTask() {
        Task task = new Task();
        task.setDueDate(now.minusDays(1));
        task.setLastModifiedAt(now.minusDays(3));

        String explanation = explanationService.generateStagnantExplanation(task, now);

        assertEquals("Overdue and inactive for 3 days", explanation);
    }
}
