package com.tasktracker.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotificationCreateRequest(
        @Email @NotBlank String email,
        @NotBlank String title,
        @NotBlank String message,
        @NotBlank String type,
        String actionUrl,
        String actionLabel
) {}
