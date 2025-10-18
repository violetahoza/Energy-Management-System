package com.vio.authorization_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @Size(min = 2, max = 50)
        String firstName,

        @Size(min = 2, max = 50)
        String lastName,

        @Email
        String email,

        @Size(max = 200)
        String address,

        @Size(min = 3, max = 50)
        String username,

        @Size(min = 6)
        String password,

        String role
) {
}
