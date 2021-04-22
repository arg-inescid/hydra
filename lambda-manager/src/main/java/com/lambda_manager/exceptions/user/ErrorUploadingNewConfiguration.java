package com.lambda_manager.exceptions.user;

public class ErrorUploadingNewConfiguration extends Exception{

    public ErrorUploadingNewConfiguration(String message) {
        super(message);
    }

    public ErrorUploadingNewConfiguration(String message, Throwable cause) {
        super(message, cause);
    }
}
