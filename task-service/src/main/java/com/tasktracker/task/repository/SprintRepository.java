package com.tasktracker.task.repository;

import com.tasktracker.task.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByBoardId(UUID boardId);
    void deleteByBoardIdIn(Iterable<UUID> boardIds);
}
