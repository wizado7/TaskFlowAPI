package com.tasktracker.user.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String userEmail,
        String fullName,
        String phone,
        String timezone,
        String avatarUrl
) {}
