package com.taskify.taskify.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Immutable DTO representing the structure of API error responses.
 */
@Schema(description = "Standard error response object")
public record ApiError(
        @Schema(description = "Timestamp of the error", example = "2026-01-14T23:07:31") LocalDateTime timestamp,

        @Schema(description = "HTTP status code", example = "400") int status,

        @Schema(description = "Error type", example = "Bad Request") String error,

        @Schema(description = "Detailed error message", example = "Validation failed for object='taskRequest'. Error count: 1") String message,

        @Schema(description = "API path where the error occurred", example = "/api/tasks") String path,

        @Schema(description = "Unique correlation ID for tracking the request", example = "550e8400-e29b-41d4-a716-446655440000") String correlationId) {
}