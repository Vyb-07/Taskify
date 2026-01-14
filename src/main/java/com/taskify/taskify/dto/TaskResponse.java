package com.taskify.taskify.dto;

import com.taskify.taskify.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO returned to clients. Contains read-only fields like id and createdAt.
 */
@Schema(description = "Response object representing a task")
public class TaskResponse {

    @Schema(description = "Unique identifier of the task", example = "1")
    private long id;

    @Schema(description = "Title of the task", example = "Complete OpenAPI documentation")
    private String title;

    @Schema(description = "Detailed description of the task", example = "Implement springdoc-openapi and document all endpoints")
    private String description;

    @Schema(description = "Current status of the task", example = "IN_PROGRESS")
    private Status status;

    @Schema(description = "Due date for the task", example = "2026-12-31T23:59:59")
    private LocalDateTime dueDate;

    @Schema(description = "Timestamp when the task was created", example = "2026-01-14T23:07:31")
    private LocalDateTime createdAt;

    public TaskResponse() {
    }

    public TaskResponse(long id, String title, String description, Status status, LocalDateTime dueDate,
            LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
    }

    // Getters only (we don't expect clients to set these)
    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}