package com.tasktracker.task.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskAttachmentResponse(
        UUID id,
        UUID taskId,
        String uploaderEmail,
        String originalFileName,
        String contentType,
        long sizeBytes,
        LocalDateTime createdAt
) {}
