package com.tasktracker.task.dto;

import com.tasktracker.task.entity.TaskPriority;
import com.tasktracker.task.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime deadline,
        List<String> assigneeEmails,
        UUID clientId,
        UUID projectId,
        UUID boardId,
        UUID columnId,
        UUID sprintId,
        boolean backlog
) {}
