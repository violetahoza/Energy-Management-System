package com.vio.authorization_service.dto;

import java.time.LocalDateTime;

/**
 * DTO that combines user profile data with credential data
 * Used by admin to see complete user information
 */
public record AdminUserResponse(
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
