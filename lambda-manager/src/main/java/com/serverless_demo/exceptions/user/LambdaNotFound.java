package com.serverless_demo.exceptions.user;

public class LambdaNotFound extends Exception {
    public LambdaNotFound(String message) {
        super(message);
    }
}
