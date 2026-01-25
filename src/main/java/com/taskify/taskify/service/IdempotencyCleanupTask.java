package com.taskify.taskify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyCleanupTask.class);
    private final IdempotencyService idempotencyService;

    public IdempotencyCleanupTask(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(cron = "${app.idempotency.cleanup-cron:0 0 0 * * *}") // Default: midnight every day
    public void cleanup() {
        logger.info("Starting scheduled cleanup of expired idempotency keys");
        idempotencyService.cleanupExpiredKeys();
        logger.info("Finished cleanup of expired idempotency keys");
    }
}
