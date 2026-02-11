package com.tasktracker.task.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BoardInviteResponse(
        UUID id,
        UUID boardId,
        String token,
        String createdBy,
        LocalDateTime expiresAt,
        boolean active
) {}
