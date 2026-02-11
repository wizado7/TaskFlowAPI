package com.tasktracker.task.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String ownerEmail,
        LocalDate startDate,
        LocalDate endDate
) {}
