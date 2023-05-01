package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.utils.Messages;

public enum LambdaType {
    // Lambda to be deployed as VM.
    VM("VM"),
    // Lambda to be deployed as container.
    CONTAINER("CONTAINER");

    private final String type;

    LambdaType(String type) {
        this.type = type;
    }

    public static LambdaType fromString(String text) throws RuntimeException {
        for (LambdaType b : LambdaType.values()) {
            if (b.type.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new RuntimeException(String.format(Messages.ERROR_LAMBDA_TYPE, text));
    }

    @Override
    public String toString() {
        return this.type;
    }
}
