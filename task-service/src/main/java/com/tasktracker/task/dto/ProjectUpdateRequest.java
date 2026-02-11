package com.tasktracker.task.dto;

import java.time.LocalDate;

public record ProjectUpdateRequest(
        String name,
        LocalDate startDate,
        LocalDate endDate
) {}
