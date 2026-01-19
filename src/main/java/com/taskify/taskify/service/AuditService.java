package com.taskify.taskify.service;

import com.taskify.taskify.model.AuditAction;
import com.taskify.taskify.model.AuditTargetType;
import java.util.Map;

public interface AuditService {
    void logEvent(AuditAction action, AuditTargetType targetType, String targetId, Map<String, Object> metadata);

    void logEvent(AuditAction action, AuditTargetType targetType, String targetId, String actorUsername,
            Map<String, Object> metadata);
}
