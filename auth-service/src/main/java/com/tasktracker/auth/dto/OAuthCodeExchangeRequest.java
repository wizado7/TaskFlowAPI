package com.tasktracker.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthCodeExchangeRequest(
        @NotBlank String code
) {
}
