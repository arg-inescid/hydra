package org.graalvm.argo.lambda_manager.exceptions.argument_parser;

public class ErrorDuringReflectiveClassCreation extends Exception {

    public ErrorDuringReflectiveClassCreation(String message, Throwable cause) {
        super(message, cause);
    }
}
