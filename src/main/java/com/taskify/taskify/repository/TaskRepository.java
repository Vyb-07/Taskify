package com.taskify.taskify.repository;

import com.taskify.taskify.model.Task;
import com.taskify.taskify.model.Status;
import com.taskify.taskify.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByStatusAndTitleContainingIgnoreCase(Status status, String title);

    List<Task> findByOwner(User owner);

    Page<Task> findByOwner(User owner, Pageable pageable);

    List<Task> findByOwnerAndStatus(User owner, Status status);

    List<Task> findByOwnerAndTitleContainingIgnoreCase(User owner, String title);

    List<Task> findByOwnerAndStatusAndTitleContainingIgnoreCase(User owner, Status status, String title);

}
