package com.taskify.taskify.service.impl;

import com.taskify.taskify.dto.DailyCheckInRequest;
import com.taskify.taskify.dto.DailyCheckInResponse;
import com.taskify.taskify.dto.TaskResponse;
import com.taskify.taskify.model.*;
import com.taskify.taskify.repository.DailyIntentRepository;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.service.AuditService;
import com.taskify.taskify.service.DailyCheckInService;
import com.taskify.taskify.service.TaskService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DailyCheckInServiceImpl implements DailyCheckInService {

    private static final Logger log = LoggerFactory.getLogger(DailyCheckInServiceImpl.class);

    private final DailyIntentRepository dailyIntentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public DailyCheckInServiceImpl(DailyIntentRepository dailyIntentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskService taskService,
            AuditService auditService,
            MeterRegistry meterRegistry) {
        this.dailyIntentRepository = dailyIntentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    @CacheEvict(value = "dailyCheckIn", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public DailyCheckInResponse checkIn(DailyCheckInRequest request) {
        User currentUser = getCurrentUser();
        LocalDate today = LocalDate.now();

        // Validate task IDs existence and ownership
        List<Task> tasks = taskRepository.findAllById(request.getTaskIds());
        if (tasks.size() != request.getTaskIds().size()) {
            throw new IllegalArgumentException("One or more invalid task IDs provided");
        }
        for (Task task : tasks) {
            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not own task ID: " + task.getId());
            }
        }

        Optional<DailyIntent> existingIntent = dailyIntentRepository.findByUserIdAndDate(currentUser.getId(), today);
        DailyIntent intent;
        AuditAction action;

        if (existingIntent.isPresent()) {
            intent = existingIntent.get();
            intent.setTaskIds(request.getTaskIds());
            intent.setNote(request.getNote());
            action = AuditAction.DAILY_CHECK_IN_UPDATE;
        } else {
            intent = new DailyIntent();
            intent.setUserId(currentUser.getId());
            intent.setDate(today);
            intent.setTaskIds(request.getTaskIds());
            intent.setNote(request.getNote());
            action = AuditAction.DAILY_CHECK_IN_CREATE;
        }

        DailyIntent saved = dailyIntentRepository.save(intent);
        auditService.logEvent(action, AuditTargetType.TASK, String.valueOf(saved.getId()), null);
        meterRegistry.counter("taskify.daily.checkins").increment();

        log.debug("User {} checked in for {}. Intent ID: {}", currentUser.getUsername(), today, saved.getId());

        return getTodayCheckIn(); // Return full today's view after check-in
    }

    @Override
    @Cacheable(value = "dailyCheckIn", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public DailyCheckInResponse getTodayCheckIn() {
        User currentUser = getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Optional<DailyIntent> todayIntent = dailyIntentRepository.findByUserIdAndDate(currentUser.getId(), today);
        Optional<DailyIntent> yesterdayIntent = dailyIntentRepository.findByUserIdAndDate(currentUser.getId(),
                yesterday);

        List<TaskResponse> todayTasks = todayIntent
                .map(intent -> mapToResponses(intent.getTaskIds()))
                .orElse(Collections.emptyList());

        List<TaskResponse> carryoverTasks = yesterdayIntent
                .map(intent -> filterCarryover(intent.getTaskIds(), currentUser.getId()))
                .orElse(Collections.emptyList());

        if (!carryoverTasks.isEmpty()) {
            meterRegistry.counter("taskify.daily.carryovers").increment();
            log.debug("User {} carried over {} tasks from yesterday.", currentUser.getUsername(),
                    carryoverTasks.size());
        }

        List<TaskResponse> suggestedTasks = taskService.getFocusTasks();

        return new DailyCheckInResponse(
                today,
                todayIntent.map(DailyIntent::getNote).orElse(null),
                todayTasks,
                carryoverTasks,
                suggestedTasks);
    }

    private List<TaskResponse> mapToResponses(List<Long> taskIds) {
        return taskRepository.findAllById(taskIds).stream()
                .filter(task -> !task.isDeleted()) // Extra safety
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private List<TaskResponse> filterCarryover(List<Long> taskIds, Long userId) {
        return taskRepository.findAllById(taskIds).stream()
                .filter(task -> task.getOwner().getId().equals(userId))
                .filter(task -> !task.isDeleted())
                .filter(task -> task.getStatus() != Status.COMPLETED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private TaskResponse mapToResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getRationale(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt());
    }
}
