package com.serverless_demo.exceptions.argument_parser;

public class ErrorDuringReflectiveClassCreation extends Exception {
    public ErrorDuringReflectiveClassCreation(String message, Throwable cause) {
        super(message, cause);
    }
}
