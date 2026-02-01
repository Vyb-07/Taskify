package com.taskify.taskify.repository;

import com.taskify.taskify.model.IntentBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntentBucketRepository extends JpaRepository<IntentBucket, Long> {
    List<IntentBucket> findAllByUserId(Long userId);

    Optional<IntentBucket> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
