package com.vio.authorization_service.handler;

public class InvalidCredentialsException extends AuthorizationException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
