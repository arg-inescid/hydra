package org.graalvm.argo.lambda_proxy.base;

/**
 * Exception thrown when polyglot function registration failed. Reasons could be 1. failure during
 * source code parsing 2. No entrypoint is found inside provided script
 */
public class FunctionRegistrationFailure extends Exception {
    public FunctionRegistrationFailure(String msg) {
        super(msg);
    }
}
