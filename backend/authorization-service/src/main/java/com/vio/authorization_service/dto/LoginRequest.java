package com.vio.authorization_service.dto;

import jakarta.validation.constraints.NotEmpty;

public record LoginRequest(
        @NotEmpty(message = "Username is required")
        String username,

        @NotEmpty(message = "Password is required")
        String password
) {
}
