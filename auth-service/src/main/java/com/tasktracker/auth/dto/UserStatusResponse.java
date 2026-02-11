package com.tasktracker.auth.dto;

import java.util.Set;
import java.util.UUID;

public record UserStatusResponse(
        UUID id,
        String email,
        Set<String> roles,
        boolean blocked
) {}
