package com.vio.device_service.handler;

public class UserServiceException extends RuntimeException {
    public UserServiceException(String message) {
        super(message);
    }

    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserServiceException(Long userId) {
        super("User with id " + userId + " does not exist or User Service is unavailable");
    }
}