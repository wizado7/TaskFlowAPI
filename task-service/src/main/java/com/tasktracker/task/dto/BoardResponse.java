package com.tasktracker.task.dto;

import com.tasktracker.task.entity.BoardType;

import java.util.UUID;

public record BoardResponse(
        UUID id,
        UUID projectId,
        String name,
        String createdBy,
        BoardType type
) {}
