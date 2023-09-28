package com.oracle.svm.graalvisor.jdbc;

public class ObjectNotFoundException extends Exception {

    private static final long serialVersionUID = -3601285192480472555L;

    private static final String MESSAGE_TYPE_ID = "No %s found with id %d.";
    private static final String MESSAGE_ISOLATE_ID = "No %s resource storage found for isolate with id %d.";
    
    public ObjectNotFoundException() {
        super("No object found.");
    }

    public ObjectNotFoundException(Class<?> klass, int objectId) {
        super(String.format(MESSAGE_TYPE_ID, klass.getName(), objectId));
    }

    public ObjectNotFoundException(long isolateId, Class<?> klass) {
        super(String.format(MESSAGE_ISOLATE_ID, klass.getName(), isolateId));
    }

}
