package com.connecthub.message.exception;

public class UnauthorizedMessageAccessException extends RuntimeException {
    public UnauthorizedMessageAccessException(String message) {
        super(message);
    }
}
