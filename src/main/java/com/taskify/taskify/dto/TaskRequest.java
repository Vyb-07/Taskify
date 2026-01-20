package com.taskify.taskify.dto;

import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO used for incoming create/update requests from clients.
 * Intentionally contains only the fields the client should supply.
 */
@Schema(description = "Request object for creating or updating a task")
public class TaskRequest {

    @Schema(description = "Title of the task", example = "Complete OpenAPI documentation", maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "title is required")
    @Size(max = 256, message = "title must be <= 255 characters")
    private String title;

    @Schema(description = "Detailed description of the task", example = "Implement springdoc-openapi and document all endpoints", maxLength = 1000)
    @Size(max = 1000, message = "description must be <= 1000 characters")
    private String description;

    @Schema(description = "Current status of the task", example = "TODO")
    private Status status;

    @Schema(description = "Priority of the task", example = "MEDIUM")
    private Priority priority;

    @Schema(description = "Due date for the task", example = "2026-12-31T23:59:59")
    @FutureOrPresent(message = "dueDate cannot be in the past")
    private LocalDateTime dueDate;

    public TaskRequest() {
    }

    // Getters & setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
}