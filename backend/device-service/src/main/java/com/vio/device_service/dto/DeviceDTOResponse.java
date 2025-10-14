package com.vio.device_service.dto;

import java.time.LocalDateTime;

public record DeviceDTOResponse(
        Long deviceId,
        String name,
        String description,
        String location,
        Double maximumConsumption,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isActive
) {
}
