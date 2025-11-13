package com.taskify.taskify.repository;

import com.taskify.taskify.model.Task;
import com.taskify.taskify.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(Status status);
    List<Task> findByTitleContainingIgnoreCase(String title);
    List<Task> findByStatusAndTitleContainingIgnoreCase(Status status, String title);

}
