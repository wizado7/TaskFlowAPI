package com.tasktracker.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record SprintRequest(
        @NotNull UUID boardId,
        @NotBlank String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active
) {}
