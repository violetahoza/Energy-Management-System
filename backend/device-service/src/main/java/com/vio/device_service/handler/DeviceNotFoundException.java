package com.vio.device_service.handler;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(Long deviceId) {
        super("Device not found with id: " + deviceId);
    }

    public DeviceNotFoundException(String message) {
        super(message);
    }
}