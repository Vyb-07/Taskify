package com.taskify.taskify.repository;

import com.taskify.taskify.model.DailyIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyIntentRepository extends JpaRepository<DailyIntent, Long> {
    Optional<DailyIntent> findByUserIdAndDate(Long userId, LocalDate date);
}
