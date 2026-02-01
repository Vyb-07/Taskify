package com.taskify.taskify.repository;

import com.taskify.taskify.model.Priority;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.Task;
import com.taskify.taskify.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TaskSpecification {

    public static Specification<Task> withStatus(Status status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> withPriority(Priority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> withIntent(Long intentId) {
        return (root, query, cb) -> intentId == null ? null : cb.equal(root.get("intentBucket").get("id"), intentId);
    }

    public static Specification<Task> withKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern));
        };
    }

    public static Specification<Task> withCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return null;
            if (start != null && end != null)
                return cb.between(root.get("createdAt"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            return cb.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }

    public static Specification<Task> withDueBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return null;
            if (start != null && end != null)
                return cb.between(root.get("dueDate"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("dueDate"), start);
            return cb.lessThanOrEqualTo(root.get("dueDate"), end);
        };
    }

    public static Specification<Task> withOwner(User owner) {
        return (root, query, cb) -> owner == null ? null : cb.equal(root.get("owner"), owner);
    }

    public static Specification<Task> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Task> isDeleted() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), true);
    }

    public static Specification<Task> isNotStatus(Status status) {
        return (root, query, cb) -> status == null ? null : cb.notEqual(root.get("status"), status);
    }

    public static Specification<Task> isStagnant(LocalDateTime now, LocalDateTime overdueThreshold,
            LocalDateTime inProgressThreshold, LocalDateTime pendingThreshold) {
        return (root, query, cb) -> cb.or(
                cb.and(cb.lessThan(root.get("dueDate"), now),
                        cb.lessThan(root.get("lastModifiedAt"), overdueThreshold)),
                cb.and(cb.equal(root.get("status"), Status.IN_PROGRESS),
                        cb.lessThan(root.get("lastModifiedAt"), inProgressThreshold)),
                cb.and(cb.equal(root.get("status"), Status.PENDING),
                        cb.lessThan(root.get("lastModifiedAt"), pendingThreshold)));
    }
}
