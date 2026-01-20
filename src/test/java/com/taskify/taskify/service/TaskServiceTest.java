package com.taskify.taskify.service;

import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.dto.TaskResponse;
import com.taskify.taskify.exception.TaskNotFoundException;
import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.Task;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.service.AuditService;
import com.taskify.taskify.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;
    private TaskRequest taskRequest;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "test@example.com", "password");
        user.setId(1L);

        task = new Task("Test Task", "Test Description", Status.PENDING, Priority.MEDIUM,
                LocalDateTime.now().plusDays(1), user);
        task.setId(1L);

        taskRequest = new TaskRequest();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Test Description");
        taskRequest.setStatus(Status.PENDING);
        taskRequest.setDueDate(LocalDateTime.now().plusDays(1));

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuthentication(String username) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    @Test
    void shouldCreateTaskSuccessfully() {
        mockAuthentication("testuser");
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.createTask(taskRequest);

        assertNotNull(response);
        assertEquals(task.getTitle(), response.getTitle());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldGetTaskByIdSuccessfully() {
        mockAuthentication("testuser");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTaskById(1L);

        assertNotNull(response);
        assertEquals(task.getTitle(), response.getTitle());
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(1L));
    }

    @Test
    void shouldUpdateTaskSuccessfully() {
        mockAuthentication("testuser");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.updateTask(1L, taskRequest);

        assertNotNull(response);
        assertEquals(taskRequest.getTitle(), response.getTitle());
    }

    @Test
    void shouldDeleteTaskSuccessfully() {
        mockAuthentication("testuser");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        taskService.deleteTask(1L);

        assertTrue(task.isDeleted());
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotOwnTask() {
        mockAuthentication("otheruser");
        User otherOwner = new User("owner", "owner@ex.com", "pass");
        otherOwner.setId(2L);
        task.setOwner(otherOwner);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(AccessDeniedException.class, () -> taskService.getTaskById(1L));
    }
}
