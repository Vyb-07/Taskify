package com.taskify.taskify.service.impl;

import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.Task;
import com.taskify.taskify.service.TaskExplanationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class TaskExplanationServiceImpl implements TaskExplanationService {

    @Override
    public String generateFocusExplanation(Task task, LocalDateTime now) {
        if (task.getDueDate() == null) {
            return "High priority task requiring immediate attention";
        }

        long daysDiff = ChronoUnit.DAYS.between(now.toLocalDate(), task.getDueDate().toLocalDate());

        if (daysDiff < 0) {
            return String.format("Overdue by %d days and high priority", Math.abs(daysDiff));
        } else if (daysDiff == 0) {
            return "Due today and high priority";
        } else {
            return String.format("Due soon (in %d days) and high priority", daysDiff);
        }
    }

    @Override
    public String generateStagnantExplanation(Task task, LocalDateTime now) {
        long inactiveDays = ChronoUnit.DAYS.between(task.getLastModifiedAt().toLocalDate(), now.toLocalDate());

        boolean isOverdue = task.getDueDate() != null && task.getDueDate().isBefore(now);

        if (isOverdue) {
            return String.format("Overdue and inactive for %d days", inactiveDays);
        }

        if (task.getStatus() == Status.IN_PROGRESS) {
            return String.format("Inactive for %d days while still IN_PROGRESS", inactiveDays);
        }

        if (task.getStatus() == Status.PENDING) {
            return String.format("Pending for %d days with no recent updates", inactiveDays);
        }

        return String.format("Inactive for %d days with no recent updates", inactiveDays);
    }
}
