package com.tasktracker.user.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 120) String fullName,
        @Size(max = 30) String phone,
        @Size(max = 60) String timezone,
        @Size(max = 255) String avatarUrl
) {}
