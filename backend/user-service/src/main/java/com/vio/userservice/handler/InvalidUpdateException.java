package com.vio.userservice.handler;

public class InvalidUpdateException extends RuntimeException {
    public InvalidUpdateException(String message) {
        super(message);
    }

    public InvalidUpdateException() {
        super("No valid fields provided for update");
    }
}