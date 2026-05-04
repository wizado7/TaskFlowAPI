package com.tasktracker.client.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientCommentResponse(
        UUID id,
        UUID clientId,
        String authorEmail,
        String message,
        Instant createdAt
) {}
