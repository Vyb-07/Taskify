package com.taskify.taskify.service.impl;

import com.taskify.taskify.dto.IntentBucketRequest;
import com.taskify.taskify.dto.IntentBucketResponse;
import com.taskify.taskify.dto.IntentOverviewResponse;
import com.taskify.taskify.model.*;
import com.taskify.taskify.repository.IntentBucketRepository;
import com.taskify.taskify.repository.TaskRepository;
import com.taskify.taskify.repository.TaskSpecification;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.service.AuditService;
import com.taskify.taskify.service.IntentBucketService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IntentBucketServiceImpl implements IntentBucketService {

        private static final Logger log = LoggerFactory.getLogger(IntentBucketServiceImpl.class);

        private final IntentBucketRepository intentBucketRepository;
        private final TaskRepository taskRepository;
        private final UserRepository userRepository;
        private final AuditService auditService;
        private final MeterRegistry meterRegistry;

        public IntentBucketServiceImpl(IntentBucketRepository intentBucketRepository,
                        TaskRepository taskRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        MeterRegistry meterRegistry) {
                this.intentBucketRepository = intentBucketRepository;
                this.taskRepository = taskRepository;
                this.userRepository = userRepository;
                this.auditService = auditService;
                this.meterRegistry = meterRegistry;
        }

        @Override
        @Transactional
        public IntentBucketResponse createIntent(IntentBucketRequest request) {
                User currentUser = getCurrentUser();

                if (intentBucketRepository.existsByUserIdAndName(currentUser.getId(), request.getName())) {
                        throw new IllegalArgumentException(
                                        "Intent bucket with name '" + request.getName() + "' already exists");
                }

                IntentBucket bucket = new IntentBucket(
                                currentUser.getId(),
                                request.getName(),
                                request.getDescription(),
                                request.getColor());

                IntentBucket saved = intentBucketRepository.save(bucket);
                auditService.logEvent(AuditAction.INTENT_BUCKET_CREATE, AuditTargetType.TASK,
                                String.valueOf(saved.getId()),
                                null);
                meterRegistry.counter("taskify.intents.created").increment();

                return mapToResponse(saved);
        }

        @Override
        public List<IntentBucketResponse> getAllIntents() {
                User currentUser = getCurrentUser();
                return intentBucketRepository.findAllByUserId(currentUser.getId()).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = "tasks", allEntries = true)
        })
        public void deleteIntent(Long id) {
                User currentUser = getCurrentUser();
                IntentBucket bucket = intentBucketRepository.findByIdAndUserId(id, currentUser.getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Intent bucket not found or access denied"));

                // Association management is handled by Task relationship (tasks keep null
                // intentBucket)
                // No explicit dissociating needed if we rely on JPA defaults or if we want to
                // be safe:
                // However, the requirement says "Tasks should simply lose the association".
                // In JPA, removing the IntentBucket will set task.intent_bucket_id to NULL if
                // properly configured.
                // Actually, since Task @ManyToOne refers to IntentBucket, we should clear the
                // references first
                // to avoid foreign key violations OR rely on ON DELETE SET NULL.
                // Let's clear them explicitly for safety and deterministic behavior.

                taskRepository.findAll(TaskSpecification.withIntent(id)).forEach(task -> {
                        task.setIntentBucket(null);
                        taskRepository.save(task);
                });

                intentBucketRepository.delete(bucket);
                auditService.logEvent(AuditAction.INTENT_BUCKET_DELETE, AuditTargetType.TASK, String.valueOf(id), null);
        }

        @Override
        @Cacheable(value = "intentOverview", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
        public IntentOverviewResponse getOverview() {
                User currentUser = getCurrentUser();
                List<IntentBucket> buckets = intentBucketRepository.findAllByUserId(currentUser.getId());
                List<IntentOverviewResponse.IntentInsight> insightList = new ArrayList<>();

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime overdueThreshold = now.minusDays(2);
                LocalDateTime inProgressThreshold = now.minusDays(3);
                LocalDateTime pendingThreshold = now.minusDays(7);

                // Selection logic for focus tasks (top 5)
                Specification<Task> focusSpec = Specification.allOf(
                                TaskSpecification.isNotDeleted(),
                                TaskSpecification.withOwner(currentUser),
                                TaskSpecification.isNotStatus(Status.COMPLETED));
                Pageable focusPageable = PageRequest.of(0, 5,
                                Sort.by(Sort.Order.asc("dueDate"), Sort.Order.desc("priority")));
                List<Task> focusTasks = taskRepository.findAll(focusSpec, focusPageable).getContent();

                for (IntentBucket bucket : buckets) {
                        if (bucket == null)
                                continue;
                        Long bid = bucket.getId();

                        long total = taskRepository.count(Specification.allOf(
                                        TaskSpecification.withOwner(currentUser),
                                        TaskSpecification.isNotDeleted(),
                                        TaskSpecification.withIntent(bid)));

                        long completed = taskRepository.count(Specification.allOf(
                                        TaskSpecification.withOwner(currentUser),
                                        TaskSpecification.isNotDeleted(),
                                        TaskSpecification.withIntent(bid),
                                        TaskSpecification.withStatus(Status.COMPLETED)));

                        long stagnant = taskRepository.count(Specification.allOf(
                                        TaskSpecification.withOwner(currentUser),
                                        TaskSpecification.isNotDeleted(),
                                        TaskSpecification.withIntent(bid),
                                        TaskSpecification.isNotStatus(Status.COMPLETED),
                                        TaskSpecification.isStagnant(now, overdueThreshold, inProgressThreshold,
                                                        pendingThreshold)));

                        long focusCount = focusTasks.stream()
                                        .filter(t -> t.getIntentBucket() != null
                                                        && t.getIntentBucket().getId().equals(bid))
                                        .count();

                        double prevalence = focusTasks.isEmpty() ? 0 : (double) focusCount / focusTasks.size();

                        insightList.add(new IntentOverviewResponse.IntentInsight(
                                        bid, bucket.getName(), total, completed, stagnant, prevalence));
                }

                log.debug("Generated Intent Overview for user: {}. Analyzed {} buckets.", currentUser.getUsername(),
                                buckets.size());
                return new IntentOverviewResponse(insightList);
        }

        private User getCurrentUser() {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Authenticated user not found: " + username));
        }

        private IntentBucketResponse mapToResponse(IntentBucket bucket) {
                return new IntentBucketResponse(
                                bucket.getId(),
                                bucket.getName(),
                                bucket.getDescription(),
                                bucket.getColor(),
                                bucket.getCreatedAt());
        }
}
