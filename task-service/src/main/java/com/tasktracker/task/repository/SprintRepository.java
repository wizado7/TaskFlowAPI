package com.tasktracker.task.repository;

import com.tasktracker.task.entity.Sprint;
import com.tasktracker.task.entity.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByBoardId(UUID boardId);
    Optional<Sprint> findByBoardIdAndStatus(UUID boardId, SprintStatus status);
    boolean existsByBoardIdAndStatus(UUID boardId, SprintStatus status);
    void deleteByBoardIdIn(Iterable<UUID> boardIds);
}
