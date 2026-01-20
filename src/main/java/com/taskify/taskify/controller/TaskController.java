package com.taskify.taskify.controller;

import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.dto.TaskResponse;
import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "APIs for managing tasks")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class TaskController {

    private final TaskService taskService;

    // Constructor injection — ensures immutability and easier testing
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Create a new task", description = "Creates a new task for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Task created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody @Valid TaskRequest request) {
        TaskResponse createdTask = taskService.createTask(request);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @Operation(summary = "Get tasks with filters", description = "Returns a paginated list of tasks filtered by various criteria")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) LocalDateTime dueFrom,
            @RequestParam(required = false) LocalDateTime dueTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TaskResponse> tasks = taskService.getAllTasks(status, priority, fromDate, toDate, dueFrom, dueTo, keyword,
                includeDeleted, pageable);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Update a task", description = "Updates an existing task by ID")
    @ApiResponse(responseCode = "200", description = "Task updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id,
            @RequestBody @Valid TaskRequest request) {
        TaskResponse updatedTask = taskService.updateTask(id, request);
        return ResponseEntity.ok(updatedTask); // HTTP 200 OK
    }

    @Operation(summary = "Delete a task", description = "Removes a task by its identifier")
    @ApiResponse(responseCode = "24", description = "Task deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}