package com.serverless_demo.exceptions.user;

public class ErrorUploadingNewLambda extends Exception {
    public ErrorUploadingNewLambda(String message) {
        super(message);
    }
    public ErrorUploadingNewLambda(String message, Throwable cause) {
        super(message, cause);
    }
}
