package com.lambda_manager.utils;

public class LambdaTuple<X, Y> {
    public final X list;
    public final Y instance;
    public LambdaTuple(X list, Y element) {
        this.list = list;
        this.instance = element;
    }
}
