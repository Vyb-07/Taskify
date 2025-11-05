package com.taskify.taskify.service;

import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.dto.TaskResponse;
import java.util.List;

public interface TaskService {

    TaskResponse createTask(TaskRequest request);
    List<TaskResponse> getAllTasks();
    TaskResponse getTaskById(Long id);
    TaskResponse updateTask(Long id, TaskRequest request);
    void deleteTask(Long id);
}
