package com.tasktracker.task.dto;

import com.tasktracker.task.entity.TaskPriority;
import com.tasktracker.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TaskRequest(
        @NotBlank String title,
        String description,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<String> assigneeEmails,
        UUID clientId,
        UUID projectId,
        UUID boardId,
        UUID columnId,
        UUID sprintId,
        Boolean backlog
) {}
