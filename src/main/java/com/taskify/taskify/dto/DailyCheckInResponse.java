package com.taskify.taskify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Response containing today's intent, carryover tasks, and focus suggestions")
public class DailyCheckInResponse {

    @Schema(description = "The date of the check-in", example = "2026-01-30")
    private LocalDate date;

    @Schema(description = "User's intent note for the day")
    private String note;

    @Schema(description = "Tasks deliberately chosen for today")
    private List<TaskResponse> todayTasks;

    @Schema(description = "Unfinished tasks carried over from yesterday's check-in")
    private List<TaskResponse> carryoverTasks;

    @Schema(description = "Suggested tasks from Focus Mode")
    private List<TaskResponse> suggestedTasks;

    public DailyCheckInResponse() {
    }

    public DailyCheckInResponse(LocalDate date, String note, List<TaskResponse> todayTasks,
            List<TaskResponse> carryoverTasks, List<TaskResponse> suggestedTasks) {
        this.date = date;
        this.note = note;
        this.todayTasks = todayTasks;
        this.carryoverTasks = carryoverTasks;
        this.suggestedTasks = suggestedTasks;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<TaskResponse> getTodayTasks() {
        return todayTasks;
    }

    public void setTodayTasks(List<TaskResponse> todayTasks) {
        this.todayTasks = todayTasks;
    }

    public List<TaskResponse> getCarryoverTasks() {
        return carryoverTasks;
    }

    public void setCarryoverTasks(List<TaskResponse> carryoverTasks) {
        this.carryoverTasks = carryoverTasks;
    }

    public List<TaskResponse> getSuggestedTasks() {
        return suggestedTasks;
    }

    public void setSuggestedTasks(List<TaskResponse> suggestedTasks) {
        this.suggestedTasks = suggestedTasks;
    }
}
