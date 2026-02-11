package com.tasktracker.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserProvisionRequest(
        @Email @NotBlank String email
) {}
