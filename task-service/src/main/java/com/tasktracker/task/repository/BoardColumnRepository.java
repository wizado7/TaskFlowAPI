package com.tasktracker.task.repository;

import com.tasktracker.task.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
    List<BoardColumn> findByBoardIdOrderByPositionAsc(UUID boardId);
    void deleteByBoardIdIn(Iterable<UUID> boardIds);
}
