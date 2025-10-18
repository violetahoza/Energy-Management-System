package com.vio.device_service.dto;

public record DeviceUpdateRequest(
    String name,
    String description,
    String location,
    Double maximumConsumption,
    Long userId
) {
}
