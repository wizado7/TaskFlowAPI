package com.tasktracker.task.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskCommentResponse(
        UUID id,
        UUID taskId,
        String authorEmail,
        String message,
        LocalDateTime createdAt
) {}
