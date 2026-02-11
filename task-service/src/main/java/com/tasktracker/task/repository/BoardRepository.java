package com.tasktracker.task.repository;

import com.tasktracker.task.entity.Board;
import com.tasktracker.task.entity.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {
    List<Board> findByProjectId(UUID projectId);
    List<Board> findByProjectIdAndType(UUID projectId, BoardType type);
    List<Board> findByProjectIdAndIdIn(UUID projectId, List<UUID> ids);
    List<Board> findByProjectIdAndTypeAndIdIn(UUID projectId, BoardType type, List<UUID> ids);
    List<Board> findByProjectIdIn(List<UUID> projectIds);
}
