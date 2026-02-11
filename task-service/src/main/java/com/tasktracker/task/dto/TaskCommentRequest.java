package com.tasktracker.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskCommentRequest(
        @NotBlank String message
) {}
