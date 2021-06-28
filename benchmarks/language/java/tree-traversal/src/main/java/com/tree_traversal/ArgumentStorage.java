package com.tree_traversal;

public class ArgumentStorage {

    // Array size.
    private int arraySize;

    private static ArgumentStorage argumentStorage;

    public static ArgumentStorage getServerArgumentStorage() {
        if (argumentStorage == null) {
            argumentStorage = new ArgumentStorage();
        }
        return argumentStorage;
    }

    public void parseArguments(String... args) {
        this.arraySize = Integer.parseInt(args[0]);
    }

    public int getArraySize() {
        return arraySize;
    }
}
