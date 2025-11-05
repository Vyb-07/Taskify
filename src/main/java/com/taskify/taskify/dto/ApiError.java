package com.taskify.taskify.dto;

import java.time.LocalDateTime;

/**
 * Immutable DTO representing the structure of API error responses.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}