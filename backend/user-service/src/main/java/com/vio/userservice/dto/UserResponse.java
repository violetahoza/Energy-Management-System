package com.vio.userservice.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String address,
        String username,
        String role,
        LocalDateTime profileCreatedAt,
        LocalDateTime profileUpdatedAt,
        LocalDateTime credentialCreatedAt,
        LocalDateTime credentialUpdatedAt
) {
}