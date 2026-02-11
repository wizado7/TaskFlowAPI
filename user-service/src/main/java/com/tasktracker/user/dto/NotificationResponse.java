package com.tasktracker.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        String type,
        String actionUrl,
        String actionLabel,
        LocalDateTime createdAt,
        boolean read
) {}
