package org.graalvm.argo.lambda_manager.exceptions.user;

public class FunctionNotFound extends Exception {

    public FunctionNotFound(String message) {
        super(message);
    }
}
