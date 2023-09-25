package com.oracle.svm.graalvisor.jdbc;

public class UnsupportedOperationException extends Exception {

    private static final long serialVersionUID = 1277828596502892139L;

    private static final String MESSAGE_OPERATION = "Unsupported operation: %s.";
    
    public UnsupportedOperationException() {
        super("Unsupported operation.");
    }

    public UnsupportedOperationException(MethodIdentifier methodIdentifier) {
        super(String.format(MESSAGE_OPERATION, methodIdentifier));
    }

}
