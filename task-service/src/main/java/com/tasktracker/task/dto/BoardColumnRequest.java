package com.tasktracker.task.dto;

import jakarta.validation.constraints.NotBlank;
public record BoardColumnRequest(
        @NotBlank String name,
        int position
) {}
