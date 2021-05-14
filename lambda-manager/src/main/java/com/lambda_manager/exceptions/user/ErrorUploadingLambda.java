package com.lambda_manager.exceptions.user;

public class ErrorUploadingLambda extends Exception {

    public ErrorUploadingLambda(String message) {
        super(message);
    }

    public ErrorUploadingLambda(String message, Throwable cause) {
        super(message, cause);
    }
}
