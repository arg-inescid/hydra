package com.serverless_demo.exceptions.argument_parser;

public class InvalidJSONFile extends Exception {
    public InvalidJSONFile(String message, Throwable cause) {
        super(message, cause);
    }
}
