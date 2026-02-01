package com.taskify.taskify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for creating or updating an intent bucket")
public class IntentBucketRequest {

    @Schema(description = "Name of the intent bucket (unique per user)", example = "Career Growth", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be <= 100 characters")
    private String name;

    @Schema(description = "Description of what this intent represents", example = "Skills, networking, and career advancement")
    @Size(max = 500, message = "description must be <= 500 characters")
    private String description;

    @Schema(description = "Optional color code for UI integration", example = "#FF5733")
    private String color;

    public IntentBucketRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
