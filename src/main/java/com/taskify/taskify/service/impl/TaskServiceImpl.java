package com.taskify.taskify.service.impl;

import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.dto.TaskResponse;
import com.taskify.taskify.exception.TaskNotFoundException;
import com.taskify.taskify.model.AuditAction;
import com.taskify.taskify.model.AuditTargetType;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.Task;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.security.SecurityConstants;
import com.taskify.taskify.service.AuditService;
import com.taskify.taskify.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
                request.getDueDate(),
                currentUser);

        Task savedTask = taskRepository.save(task);

        auditService.logEvent(AuditAction.TASK_CREATE, AuditTargetType.TASK, String.valueOf(savedTask.getId()), null);

        return mapToResponse(savedTask);
    }

    @Override
    public List<TaskResponse> getAllTasks() {
        User user = getCurrentUser();
        List<Task> tasks;
        if (isAdmin(user)) {
            tasks = taskRepository.findAll();
        } else {
            tasks = taskRepository.findByOwner(user);
        }
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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

        taskRepository.delete(task);

        auditService.logEvent(AuditAction.TASK_DELETE, AuditTargetType.TASK, String.valueOf(id),
                isAdminAction ? java.util.Map.of("adminAction", true) : null);
    }

    @Override
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        User user = getCurrentUser();
        Page<Task> tasks;
        if (isAdmin(user)) {
            tasks = taskRepository.findAll(pageable);
        } else {
            tasks = taskRepository.findByOwner(user, pageable);
        }
        return tasks.map(this::mapToResponse);
    }

    @Override
    public List<TaskResponse> getTasksByFilter(String title, Status status) {
        User user = getCurrentUser();
        List<Task> tasks;

        if (isAdmin(user)) {
            if (title != null && status != null) {
                tasks = taskRepository.findByStatusAndTitleContainingIgnoreCase(status, title);
            } else if (status != null) {
                // Fixed missing findByStatus in repository
                tasks = taskRepository.findAll().stream()
                        .filter(t -> t.getStatus() == status)
                        .collect(Collectors.toList());
            } else if (title != null) {
                // Fixed missing findByTitleContainingIgnoreCase in repository
                tasks = taskRepository.findAll().stream()
                        .filter(t -> t.getTitle().toLowerCase().contains(title.toLowerCase()))
                        .collect(Collectors.toList());
            } else {
                tasks = taskRepository.findAll();
            }
        } else {
            if (title != null && status != null) {
                tasks = taskRepository.findByOwnerAndStatusAndTitleContainingIgnoreCase(user, status, title);
            } else if (status != null) {
                tasks = taskRepository.findByOwnerAndStatus(user, status);
            } else if (title != null) {
                tasks = taskRepository.findByOwnerAndTitleContainingIgnoreCase(user, title);
            } else {
                tasks = taskRepository.findByOwner(user);
            }
        }

        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
                task.getDueDate(),
                task.getCreatedAt());
    }
}
