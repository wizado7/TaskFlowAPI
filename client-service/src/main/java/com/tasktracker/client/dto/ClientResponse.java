package com.tasktracker.client.dto;

import java.util.UUID;

public record ClientResponse(
        UUID id,
        UUID projectId,
        String name,
        String email,
        String phone,
        String company,
        String notes,
        String stage,
        String createdBy
) {}
