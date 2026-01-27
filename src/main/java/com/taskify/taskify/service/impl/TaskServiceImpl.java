package com.taskify.taskify.service.impl;

import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.dto.TaskResponse;
import com.taskify.taskify.exception.TaskNotFoundException;
import com.taskify.taskify.model.*;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.TaskSpecification;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.security.SecurityConstants;
import com.taskify.taskify.service.AuditService;
import com.taskify.taskify.service.TaskService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository,
            AuditService auditService, CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        User currentUser = getCurrentUser();
        Task task = new Task(
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getPriority(),
                request.getDueDate(),
                currentUser);

        Task savedTask = taskRepository.save(task);

        auditService.logEvent(AuditAction.TASK_CREATE, AuditTargetType.TASK, String.valueOf(savedTask.getId()), null);

        meterRegistry.counter("taskify.tasks.created").increment();

        incrementTaskVersion(currentUser.getUsername());

        return mapToResponse(savedTask);
    }

    @Override
    @Cacheable(value = "taskDetails", key = "#id", unless = "#result == null")
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        validateOwnership(task);

        return mapToResponse(task);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "taskDetails", key = "#id")
    })
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        validateOwnership(existingTask);

        existingTask.setTitle(request.getTitle());
        existingTask.setDescription(request.getDescription());
        existingTask.setStatus(request.getStatus());
        existingTask.setDueDate(request.getDueDate());

        try {
            Task updatedTask = taskRepository.save(existingTask);

            User currentUser = getCurrentUser();
            boolean isAdminAction = isAdmin(currentUser) && existingTask.getOwner().getId() != currentUser.getId();
            auditService.logEvent(AuditAction.TASK_UPDATE, AuditTargetType.TASK, String.valueOf(updatedTask.getId()),
                    isAdminAction ? java.util.Map.of("adminAction", true) : null);

            incrementTaskVersion(existingTask.getOwner().getUsername());
            if (isAdminAction) {
                incrementTaskVersion(currentUser.getUsername());
            }

            meterRegistry.counter("taskify.tasks.updated").increment();

            return mapToResponse(updatedTask);
        } catch (ObjectOptimisticLockingFailureException e) {
            meterRegistry.counter("taskify.tasks.optimistic_lock_conflicts").increment();
            throw e;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "taskDetails", key = "#id")
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        validateOwnership(task);

        User currentUser = getCurrentUser();
        boolean isAdminAction = isAdmin(currentUser) && task.getOwner().getId() != currentUser.getId();

        task.setDeleted(true);
        taskRepository.save(task);

        auditService.logEvent(AuditAction.TASK_DELETE, AuditTargetType.TASK, String.valueOf(id),
                isAdminAction ? java.util.Map.of("adminAction", true) : null);

        incrementTaskVersion(task.getOwner().getUsername());
        if (isAdminAction) {
            incrementTaskVersion(currentUser.getUsername());
        }
    }

    @Override
    public List<TaskResponse> getFocusTasks() {
        User currentUser = getCurrentUser();

        Specification<Task> spec = Specification.allOf(
                TaskSpecification.isNotDeleted(),
                TaskSpecification.withOwner(currentUser),
                TaskSpecification.isNotStatus(Status.COMPLETED));

        // Selection logic: Overdue/Due Soon first (ASC), then High Priority (DESC)
        Pageable pageable = PageRequest.of(0, 5, Sort.by(
                Sort.Order.asc("dueDate"),
                Sort.Order.desc("priority")));

        List<Task> focusTasks = taskRepository.findAll(spec, pageable).getContent();

        log.debug("Focus Mode used by user: {}. Selected {} tasks based on urgency and priority.",
                currentUser.getUsername(), focusTasks.size());

        meterRegistry.counter("taskify.tasks.focus_mode_usage").increment();

        return focusTasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "tasks", keyGenerator = "taskCacheKeyGenerator")
    public Page<TaskResponse> getAllTasks(
            Status status,
            Priority priority,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            LocalDateTime dueFrom,
            LocalDateTime dueTo,
            String keyword,
            boolean includeDeleted,
            Pageable pageable) {

        User currentUser = getCurrentUser();
        boolean isAdmin = isAdmin(currentUser);

        Specification<Task> spec = Specification.where(null);

        // 1. Ownership Visibility
        if (!isAdmin) {
            spec = spec.and(TaskSpecification.withOwner(currentUser));
        }

        // 2. Soft Delete Visibility
        if (includeDeleted) {
            if (!isAdmin) {
                throw new AccessDeniedException("Only admins can fetch deleted tasks");
            }
            spec = spec.and(TaskSpecification.isDeleted());
        } else {
            spec = spec.and(TaskSpecification.isNotDeleted());
        }

        // 3. Optional Filters
        spec = spec.and(TaskSpecification.withStatus(status))
                .and(TaskSpecification.withPriority(priority))
                .and(TaskSpecification.withKeyword(keyword))
                .and(TaskSpecification.withCreatedBetween(fromDate, toDate))
                .and(TaskSpecification.withDueBetween(dueFrom, dueTo));

        Page<Task> tasks = taskRepository.findAll(spec, pageable);
        return tasks.map(this::mapToResponse);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private void validateOwnership(Task task) {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser) && task.getOwner().getId() != currentUser.getId()) {
            throw new AccessDeniedException("You do not have permission to access this task");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(SecurityConstants.ROLE_ADMIN));
    }

    private void incrementTaskVersion(String username) {
        Cache versionCache = cacheManager.getCache("taskVersions");
        if (versionCache != null) {
            String currentVersion = versionCache.get(username, String.class);
            long nextVersion = (currentVersion == null) ? 1 : Long.parseLong(currentVersion) + 1;
            versionCache.put(username, String.valueOf(nextVersion));
        }
    }

    private TaskResponse mapToResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt());
    }
}
