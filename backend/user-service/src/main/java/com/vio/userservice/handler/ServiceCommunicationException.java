package com.vio.userservice.handler;

/**
 * Exception thrown when communication with another microservice fails
 */
public class ServiceCommunicationException extends RuntimeException {

    private final String serviceName;

    public ServiceCommunicationException(String serviceName, String message) {
        super(String.format("Failed to communicate with %s: %s", serviceName, message));
        this.serviceName = serviceName;
    }

    public ServiceCommunicationException(String serviceName, String message, Throwable cause) {
        super(String.format("Failed to communicate with %s: %s", serviceName, message), cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}