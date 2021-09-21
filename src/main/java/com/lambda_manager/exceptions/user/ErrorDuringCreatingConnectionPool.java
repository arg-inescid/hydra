package com.lambda_manager.exceptions.user;

public class ErrorDuringCreatingConnectionPool extends Exception {

    public ErrorDuringCreatingConnectionPool(String message) {
        super(message);
    }

    public ErrorDuringCreatingConnectionPool(String message, Throwable cause) {
        super(message, cause);
    }
}
