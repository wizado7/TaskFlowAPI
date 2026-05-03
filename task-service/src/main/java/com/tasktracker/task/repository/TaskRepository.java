package com.tasktracker.task.repository;

import com.tasktracker.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByAssigneeEmailsContaining(String assigneeEmail);
    List<Task> findByBoardIdAndBacklogTrue(UUID boardId);
    List<Task> findByBoardId(UUID boardId);
    List<Task> findByBoardIdAndSprintId(UUID boardId, UUID sprintId);
    List<Task> findBySprintId(UUID sprintId);
    List<Task> findByBoardIdIn(List<UUID> boardIds);
    List<Task> findByProjectIdIn(List<UUID> projectIds);
    void deleteByBoardIdIn(Iterable<UUID> boardIds);
    void deleteByProjectId(UUID projectId);
}
