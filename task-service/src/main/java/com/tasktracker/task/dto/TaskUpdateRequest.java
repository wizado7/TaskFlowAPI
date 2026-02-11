package com.tasktracker.task.dto;

import com.tasktracker.task.entity.TaskPriority;
import com.tasktracker.task.entity.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskUpdateRequest(
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        List<String> assigneeEmails,
        UUID clientId,
        UUID projectId,
        UUID boardId,
        UUID columnId,
        UUID sprintId,
        Boolean backlog
) {}
