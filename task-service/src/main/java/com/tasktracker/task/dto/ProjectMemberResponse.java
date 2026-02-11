package com.tasktracker.task.dto;

import com.tasktracker.task.entity.ProjectRole;

import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID projectId,
        String userEmail,
        ProjectRole role
) {}
