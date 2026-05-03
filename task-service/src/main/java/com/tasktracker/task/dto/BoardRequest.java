package com.tasktracker.task.dto;

import com.tasktracker.task.entity.BoardType;
import com.tasktracker.task.entity.BoardMethodology;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BoardRequest(
        @NotNull UUID projectId,
        @NotBlank String name,
        BoardType type,
        BoardMethodology methodology,
        LocalDate sprintStartDate,
        LocalDate sprintEndDate,
        List<UUID> backlogTaskIds
) {}
