package com.vio.authorization_service.handler;

public class CredentialNotFoundException extends AuthorizationException {
    public CredentialNotFoundException(String message) {
        super(message);
    }

    public CredentialNotFoundException(Long userId) {
        super("Credentials not found for userId: " + userId);
    }
}