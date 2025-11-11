package com.taskify.taskify.dto;

import com.taskify.taskify.model.Status;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO used for incoming create/update requests from clients.
 * Intentionally contains only the fields the client should supply.
 */
public class TaskRequest {
    @NotBlank(message = "title is required")
    @Size(max = 256, message = "title must be <= 255 characters")
    private String title;

    @Size(max = 1000, message = "description must be <= 1000 characters")
    private String description;

    private Status status;

    @FutureOrPresent(message = "dueDate cannot be in the past")
    private LocalDateTime dueDate;

    public TaskRequest() {}

    // Getters & setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}