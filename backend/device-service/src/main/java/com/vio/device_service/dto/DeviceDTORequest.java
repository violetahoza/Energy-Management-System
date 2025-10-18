package com.vio.device_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeviceDTORequest(
        @NotEmpty(message = "Device name must not be empty")
        String name,

        String description,

        @NotEmpty(message = "Location must not be empty")
        String location,

        @NotNull(message = "Maximum consumption must not be null")
        @Positive(message = "Maximum consumption must be positive")
        Double maximumConsumption,

        Long userId
) {
}
