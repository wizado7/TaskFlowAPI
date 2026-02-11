package com.tasktracker.task.dto;

import com.tasktracker.task.entity.ProjectRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectInviteResponse(
        UUID id,
        UUID projectId,
        String email,
        ProjectRole role,
        String token,
        String createdBy,
        LocalDateTime expiresAt,
        boolean active
) {}
