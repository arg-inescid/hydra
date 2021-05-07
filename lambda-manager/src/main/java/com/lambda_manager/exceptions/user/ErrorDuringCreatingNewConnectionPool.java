package com.lambda_manager.exceptions.user;

public class ErrorDuringCreatingNewConnectionPool extends Exception{

    public ErrorDuringCreatingNewConnectionPool(String message) {
        super(message);
    }

    public ErrorDuringCreatingNewConnectionPool(String message, Throwable cause) {
        super(message, cause);
    }
}
