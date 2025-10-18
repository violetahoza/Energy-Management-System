package com.vio.authorization_service.dto;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        String role,
        String message
) {
}
