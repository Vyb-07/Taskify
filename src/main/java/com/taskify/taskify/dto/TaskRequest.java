package com.taskify.taskify.dto;

import com.taskify.taskify.model.Status;
import java.time.LocalDateTime;

/**
 * DTO used for incoming create/update requests from clients.
 * Intentionally contains only the fields the client should supply.
 */
public class TaskRequest {
    private String title;
    private String description;
    private Status status;
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