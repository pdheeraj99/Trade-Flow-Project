package com.tradeflow.auth.exception;

/**
 * Exception thrown when refresh token is invalid or expired
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
