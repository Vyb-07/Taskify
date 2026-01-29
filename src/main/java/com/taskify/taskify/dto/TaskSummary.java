package com.taskify.taskify.dto;

public class TaskSummary {
    private final long createdTasks;
    private final long completedTasks;
    private final long stagnantTasks;
    private final long overdueTasks;
    private final long focusModeUsages;

    public TaskSummary(long createdTasks, long completedTasks, long stagnantTasks, long overdueTasks,
            long focusModeUsages) {
        this.createdTasks = createdTasks;
        this.completedTasks = completedTasks;
        this.stagnantTasks = stagnantTasks;
        this.overdueTasks = overdueTasks;
        this.focusModeUsages = focusModeUsages;
    }

    public long getCreatedTasks() {
        return createdTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public long getStagnantTasks() {
        return stagnantTasks;
    }

    public long getOverdueTasks() {
        return overdueTasks;
    }

    public long getFocusModeUsages() {
        return focusModeUsages;
    }
}
