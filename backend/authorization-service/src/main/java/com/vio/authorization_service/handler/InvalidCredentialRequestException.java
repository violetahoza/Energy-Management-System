package com.vio.authorization_service.handler;

public class InvalidCredentialRequestException extends RuntimeException {
    public InvalidCredentialRequestException(String message) {
        super(message);
    }
}
