package com.taskify.taskify.dto;

import java.util.List;

public class TaskReviewResponse {
    private final String period;
    private final TaskSummary summary;
    private final List<String> insights;

    public TaskReviewResponse(String period, TaskSummary summary, List<String> insights) {
        this.period = period;
        this.summary = summary;
        this.insights = insights;
    }

    public String getPeriod() {
        return period;
    }

    public TaskSummary getSummary() {
        return summary;
    }

    public List<String> getInsights() {
        return insights;
    }
}
