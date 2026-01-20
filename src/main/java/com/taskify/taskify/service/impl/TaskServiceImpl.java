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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository, AuditService auditService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
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

        return mapToResponse(savedTask);
    }

    @Override
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        validateOwnership(task);

        return mapToResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        validateOwnership(existingTask);

        existingTask.setTitle(request.getTitle());
        existingTask.setDescription(request.getDescription());
        existingTask.setStatus(request.getStatus());
        existingTask.setDueDate(request.getDueDate());

        Task updatedTask = taskRepository.save(existingTask);

        User currentUser = getCurrentUser();
        boolean isAdminAction = isAdmin(currentUser) && existingTask.getOwner().getId() != currentUser.getId();
        auditService.logEvent(AuditAction.TASK_UPDATE, AuditTargetType.TASK, String.valueOf(updatedTask.getId()),
                isAdminAction ? java.util.Map.of("adminAction", true) : null);

        return mapToResponse(updatedTask);
    }

    @Override
    @Transactional
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
    }

    @Override
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
