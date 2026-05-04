package com.tasktracker.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClientCreateRequest(
        @NotNull UUID projectId,
        @NotBlank String name,
        String email,
        String phone,
        String company,
        String notes,
        String stage
) {}
