package com.vio.userservice.dto;

import com.vio.userservice.model.UserRole;

import java.time.LocalDateTime;

public record UserDTOResponse(
        Long userId,
        String username,
        String firstName,
        String lastName,
        String email,
        String address,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
