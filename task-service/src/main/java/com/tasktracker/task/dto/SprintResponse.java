package com.tasktracker.task.dto;

import java.time.LocalDate;
import java.util.UUID;

public record SprintResponse(
        UUID id,
        UUID boardId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {}
