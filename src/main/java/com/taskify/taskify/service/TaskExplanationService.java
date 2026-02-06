package com.taskify.taskify.service;

import com.taskify.taskify.model.Task;
import java.time.LocalDateTime;

/**
 * Service responsible for generating human-readable explanations for tasks.
 */
public interface TaskExplanationService {

    /**
     * Generates an explanation for why a task is included in Focus Mode.
     */
    String generateFocusExplanation(Task task, LocalDateTime now);

    /**
     * Generates an explanation for why a task is flagged as stagnant.
     */
    String generateStagnantExplanation(Task task, LocalDateTime now);
}
