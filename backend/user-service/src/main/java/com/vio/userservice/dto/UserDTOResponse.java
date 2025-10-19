package com.vio.userservice.dto;

import java.time.LocalDateTime;

public record UserDTOResponse(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
