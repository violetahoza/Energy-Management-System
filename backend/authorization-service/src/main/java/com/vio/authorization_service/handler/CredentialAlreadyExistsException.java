package com.vio.authorization_service.handler;

public class CredentialAlreadyExistsException extends AuthorizationException {
    public CredentialAlreadyExistsException(String message) {
        super(message);
    }
}