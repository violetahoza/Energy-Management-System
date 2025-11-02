package com.vio.userservice.handler;

public class InvalidUserCreationException extends RuntimeException {
    public InvalidUserCreationException(String message) {
        super(message);
    }
}
