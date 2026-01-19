package com.taskify.taskify.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.model.AuditAction;
import com.taskify.taskify.model.AuditLog;
import com.taskify.taskify.model.AuditTargetType;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.AuditLogRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @Override
    public void logEvent(AuditAction action, AuditTargetType targetType, String targetId,
            Map<String, Object> metadata) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actorUsername = (authentication != null && authentication.isAuthenticated()) ? authentication.getName()
                : null;
        logEventInternal(action, targetType, targetId, actorUsername, metadata);
    }

    @Async
    @Override
    public void logEvent(AuditAction action, AuditTargetType targetType, String targetId, String actorUsername,
            Map<String, Object> metadata) {
        logEventInternal(action, targetType, targetId, actorUsername, metadata);
    }

    private void logEventInternal(AuditAction action, AuditTargetType targetType, String targetId, String actorUsername,
            Map<String, Object> metadata) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);

            if (actorUsername != null) {
                Optional<User> actor = userRepository.findByUsername(actorUsername);
                actor.ifPresent(user -> {
                    auditLog.setActorUserId(user.getId());
                    String roles = user.getRoles().stream()
                            .map(r -> r.getName())
                            .collect(Collectors.joining(","));
                    auditLog.setActorRole(roles);
                });
            }

            auditLog.setIpAddress(getClientIp());

            if (metadata != null && !metadata.isEmpty()) {
                try {
                    auditLog.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                    logger.error("Failed to serialize audit metadata", e);
                }
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Requirement 2: Audit logging must NOT break the main request flow
            logger.error("Error creating audit log entry", e);
        }
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader == null) {
                return request.getRemoteAddr();
            }
            return xfHeader.split(",")[0];
        }
        return null;
    }
}
