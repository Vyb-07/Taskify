package com.taskify.taskify.service;

import com.taskify.taskify.dto.TaskRequest;
import com.taskify.taskify.dto.TaskResponse;
import com.taskify.taskify.dto.TaskReviewResponse;
import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskService {

    TaskResponse createTask(TaskRequest request);

    TaskResponse getTaskById(Long id);

    TaskResponse updateTask(Long id, TaskRequest request);

    void deleteTask(Long id);

    List<TaskResponse> getFocusTasks();

    Page<TaskResponse> getAllTasks(
            Status status,
            Priority priority,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            LocalDateTime dueFrom,
            LocalDateTime dueTo,
            String keyword,
            boolean includeDeleted,
            Pageable pageable);

    List<TaskResponse> getStagnantTasks();

    TaskReviewResponse getWeeklyReview();
}
