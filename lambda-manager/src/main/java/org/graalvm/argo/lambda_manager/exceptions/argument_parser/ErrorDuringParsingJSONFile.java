package org.graalvm.argo.lambda_manager.exceptions.argument_parser;

public class ErrorDuringParsingJSONFile extends Exception {

    public ErrorDuringParsingJSONFile(String message, Throwable cause) {
        super(message, cause);
    }
}
