package com.tasktracker.task.dto;

import com.tasktracker.task.entity.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BoardRequest(
        @NotNull UUID projectId,
        @NotBlank String name,
        BoardType type
) {}
