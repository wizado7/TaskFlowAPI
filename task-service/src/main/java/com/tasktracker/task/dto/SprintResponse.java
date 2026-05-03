package com.tasktracker.task.dto;

import com.tasktracker.task.entity.SprintStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SprintResponse(
        UUID id,
        UUID boardId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        String goal,
        Integer capacityPoints,
        SprintStatus status,
        LocalDateTime completedAt,
        int plannedPoints,
        int completedPoints
) {}
