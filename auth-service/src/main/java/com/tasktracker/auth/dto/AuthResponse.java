package com.tasktracker.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        long refreshExpiresInSeconds
) {}
