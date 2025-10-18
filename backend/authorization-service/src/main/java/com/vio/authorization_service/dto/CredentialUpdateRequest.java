package com.vio.authorization_service.dto;

import jakarta.validation.constraints.Size;

public record CredentialUpdateRequest(
        Long userId,

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        String role
) {
}