package com.vio.authorization_service.handler;

public class UsernameAlreadyExistsException extends AuthorizationException {
  public UsernameAlreadyExistsException(String username) {
    super("Username already exists: " + username);
  }
}