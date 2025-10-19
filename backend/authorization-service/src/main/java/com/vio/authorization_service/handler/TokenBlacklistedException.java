package com.vio.authorization_service.handler;

public class TokenBlacklistedException extends AuthorizationException {
    public TokenBlacklistedException() {
        super("Token has been revoked");
    }
}