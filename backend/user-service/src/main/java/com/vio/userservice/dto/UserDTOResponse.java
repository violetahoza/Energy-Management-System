package com.vio.userservice.dto;

import com.vio.userservice.model.UserRole;

import java.time.LocalDateTime;

public record UserDTOResponse(
        Long userId,
        String name,
        String email,
        String address,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
