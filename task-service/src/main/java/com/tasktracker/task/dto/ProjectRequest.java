package com.tasktracker.task.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank String name,
        LocalDate startDate,
        LocalDate endDate
) {}
