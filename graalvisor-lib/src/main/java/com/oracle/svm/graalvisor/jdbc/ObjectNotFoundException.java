package com.oracle.svm.graalvisor.jdbc;

public class ObjectNotFoundException extends Exception {

    private static final long serialVersionUID = -3601285192480472555L;

    private static final String MESSAGE_TYPE_ID = "No %s found with id %d.";
    
    public ObjectNotFoundException() {
        super("No object found.");
    }

    public ObjectNotFoundException(Class<?> klass, long id) {
        super(String.format(MESSAGE_TYPE_ID, klass.getName(), id));
    }

}
