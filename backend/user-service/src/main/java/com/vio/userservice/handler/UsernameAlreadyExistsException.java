package com.vio.userservice.handler;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
      super("Email: " + username + " is already in use.");
    }
}
