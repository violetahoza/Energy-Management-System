package com.vio.authorization_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CredentialRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotEmpty(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Size(min = 6, message = "Password must be at least 6 characters")
        String password, // Optional for updates

        @NotEmpty(message = "Role is required")
        String role // CLIENT or ADMIN
) {
}
