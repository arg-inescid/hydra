package com.serverless_demo.exceptions.argument_parser;

public class ErrorDuringParsingJSONFile extends Exception {
    public ErrorDuringParsingJSONFile(String message, Throwable cause) {
        super(message, cause);
    }
}
