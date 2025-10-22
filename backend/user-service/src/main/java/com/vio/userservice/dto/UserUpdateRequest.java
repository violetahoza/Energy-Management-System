package com.vio.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        // Profile data (all optional)
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @Email(message = "Invalid email format")
        String email,

        @Size(max = 200, message = "Address must not exceed 200 characters")
        String address,

        // Credential data (all optional)
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        String role // CLIENT or ADMIN
) {
}