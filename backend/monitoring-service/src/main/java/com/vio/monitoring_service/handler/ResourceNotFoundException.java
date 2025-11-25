package com.vio.monitoring_service.handler;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(Long id, String resourceName) {
        super(resourceName + " not found with id: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}