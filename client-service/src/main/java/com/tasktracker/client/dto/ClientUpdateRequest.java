package com.tasktracker.client.dto;

public record ClientUpdateRequest(
        String name,
        String email,
        String phone,
        String company,
        String notes
) {}
