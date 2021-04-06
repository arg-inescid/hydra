package com.lambda_manager.utils;

// TODO - this tuple is specific to list and instance. The name is misleading.
public class Tuple<X, Y> {
    public final X list;
    public final Y instance;
    public Tuple(X list, Y element) {
        this.list = list;
        this.instance = element;
    }
}
