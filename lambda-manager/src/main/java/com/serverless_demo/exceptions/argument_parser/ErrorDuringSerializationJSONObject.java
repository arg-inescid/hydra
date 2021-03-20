package com.serverless_demo.exceptions.argument_parser;

public class ErrorDuringSerializationJSONObject extends Exception {
    public ErrorDuringSerializationJSONObject(String message, Throwable cause) {
        super(message, cause);
    }
}
