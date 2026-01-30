package com.taskify.taskify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Request to create or update a daily check-in")
public class DailyCheckInRequest {

    @Schema(description = "List of 1 to 3 task IDs to focus on", example = "[1, 2, 3]")
    @Size(min = 1, max = 3, message = "Choose between 1 and 3 tasks for your daily check-in")
    private List<Long> taskIds;

    @Schema(description = "Optional note describing the intent for these tasks", example = "Deep work on core logic")
    private String note;

    public DailyCheckInRequest() {
    }

    public List<Long> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<Long> taskIds) {
        this.taskIds = taskIds;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
