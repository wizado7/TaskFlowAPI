package com.tasktracker.client.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientCreateRequest(
        @NotBlank String name,
        String email,
        String phone,
        String company,
        String notes
) {}
