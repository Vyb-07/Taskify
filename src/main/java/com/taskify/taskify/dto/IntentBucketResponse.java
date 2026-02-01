package com.taskify.taskify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response object representing an intent bucket")
public class IntentBucketResponse {

    private Long id;
    private String name;
    private String description;
    private String color;
    private LocalDateTime createdAt;

    public IntentBucketResponse() {
    }

    public IntentBucketResponse(Long id, String name, String description, String color, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
