package com.taskify.taskify.repository;

import com.taskify.taskify.model.AuditAction;
import com.taskify.taskify.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    long countByActorUserIdAndActionAndTimestampBetween(Long userId, AuditAction action, LocalDateTime start,
            LocalDateTime end);
}
