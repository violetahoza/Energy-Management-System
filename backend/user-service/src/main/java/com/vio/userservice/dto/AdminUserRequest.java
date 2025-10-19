package com.vio.userservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

// DTO for admin to create a complete user with both profile and credentials
public record AdminUserRequest(
        // Profile data
        @NotEmpty(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @NotEmpty(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @NotEmpty(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Size(max = 200, message = "Address must not exceed 200 characters")
        String address,

        // Credential data
        @NotEmpty(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotEmpty(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotEmpty(message = "Role is required")
        String role // CLIENT or ADMIN
) {
}
