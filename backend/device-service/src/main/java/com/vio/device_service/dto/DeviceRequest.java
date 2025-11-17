package com.vio.device_service.dto;

import jakarta.validation.constraints.Positive;

public record DeviceRequest(
        String name,

        String description,

        String location,

        @Positive(message = "Maximum consumption must be positive")
        Double maximumConsumption,

        Long userId
) {
}