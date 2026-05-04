package com.tasktracker.client.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientCommentRequest(
        @NotBlank String message
) {}
