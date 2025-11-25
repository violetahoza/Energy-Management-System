package com.vio.monitoring_service.handler;

public class InvalidQueryException extends RuntimeException {
    public InvalidQueryException(String message) {
        super(message);
    }
}