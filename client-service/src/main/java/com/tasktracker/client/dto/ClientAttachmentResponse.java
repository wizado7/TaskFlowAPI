package com.tasktracker.client.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientAttachmentResponse(
        UUID id,
        UUID clientId,
        String uploaderEmail,
        String originalFileName,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {}
