package com.taskify.taskify.controller;

import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.dto.TaskResponse;
import com.taskify.taskify.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    // Constructor injection — ensures immutability and easier testing
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // POST /api/tasks
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody TaskRequest request) {
        TaskResponse createdTask = taskService.createTask(request);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    // GET /api/tasks
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<TaskResponse> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        TaskResponse task = taskService.getTaskById(id);
        return ResponseEntity.ok(task); // 200 OK
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id,
                                                   @RequestBody TaskRequest request) {
        TaskResponse updatedTask = taskService.updateTask(id, request);
        return ResponseEntity.ok(updatedTask); // HTTP 200 OK
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id){
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}