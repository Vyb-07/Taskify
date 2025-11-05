package com.taskify.taskify.dto;

import com.taskify.taskify.model.Status;
import java.time.LocalDateTime;

/**
 * DTO returned to clients. Contains read-only fields like id and createdAt.
 */
public class TaskResponse {
    private long id;
    private String title;
    private String description;
    private Status status;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;

    public TaskResponse() {}

    public TaskResponse(long id, String title, String description, Status status, LocalDateTime dueDate, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
    }

    // Getters only (we don't expect clients to set these)
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public LocalDateTime getDueDate() { return dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}