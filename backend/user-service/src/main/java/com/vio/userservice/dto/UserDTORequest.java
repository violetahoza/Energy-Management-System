package com.vio.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UserDTORequest(
//        @NotEmpty(message = "Username is required")
//        @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
//        String username,

        @NotEmpty(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @NotEmpty(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @NotEmpty(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Size(max = 200, message = "Address must not exceed 200 characters")
        String address

//        @NotNull(message = "Role is required")
//        String role
) {
}
