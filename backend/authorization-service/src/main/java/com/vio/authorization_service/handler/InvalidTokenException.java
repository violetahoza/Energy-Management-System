package com.vio.authorization_service.handler;

public class InvalidTokenException extends AuthorizationException {
  public InvalidTokenException(String message) {
    super(message);
  }
}