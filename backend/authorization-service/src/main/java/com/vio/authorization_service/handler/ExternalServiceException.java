package com.vio.authorization_service.handler;

public class ExternalServiceException extends AuthorizationException {
    public ExternalServiceException(String serviceName, String message) {
        super("Error communicating with " + serviceName + ": " + message);
    }

    public ExternalServiceException(String serviceName, Throwable cause) {
        super("Error communicating with " + serviceName + ": " + cause.getMessage(), cause);
    }
}