package com.tasktracker.task.dto;

import com.tasktracker.task.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProjectInviteRequest(
        @Email @NotBlank String email,
        ProjectRole role
) {}
