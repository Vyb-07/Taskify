package com.taskify.taskify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated insights for intent buckets")
public class IntentOverviewResponse {

    private List<IntentInsight> insights;

    public IntentOverviewResponse() {
    }

    public IntentOverviewResponse(List<IntentInsight> insights) {
        this.insights = insights;
    }

    public List<IntentInsight> getInsights() {
        return insights;
    }

    public void setInsights(List<IntentInsight> insights) {
        this.insights = insights;
    }

    public static class IntentInsight {
        private Long id;
        private String name;
        private long totalTasks;
        private long completedTasks;
        private long stagnantTasks;
        private double focusPrevalence; // prevalence in focus mode

        public IntentInsight() {
        }

        public IntentInsight(Long id, String name, long totalTasks, long completedTasks, long stagnantTasks,
                double focusPrevalence) {
            this.id = id;
            this.name = name;
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.stagnantTasks = stagnantTasks;
            this.focusPrevalence = focusPrevalence;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public long getTotalTasks() {
            return totalTasks;
        }

        public long getCompletedTasks() {
            return completedTasks;
        }

        public long getStagnantTasks() {
            return stagnantTasks;
        }

        public double getFocusPrevalence() {
            return focusPrevalence;
        }
    }
}
