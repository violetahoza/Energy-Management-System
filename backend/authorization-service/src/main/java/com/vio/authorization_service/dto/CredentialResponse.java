package com.vio.authorization_service.dto;

import java.time.LocalDateTime;

public record CredentialResponse(
        Long id,
        Long userId,
        String username,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
