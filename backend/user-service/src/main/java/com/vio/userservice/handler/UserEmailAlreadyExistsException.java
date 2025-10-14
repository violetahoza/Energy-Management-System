package com.vio.userservice.handler;

public class UserEmailAlreadyExistsException extends RuntimeException {
    public UserEmailAlreadyExistsException(String email) {
        super("Email: " + email + " is already in use.");
    }
}
