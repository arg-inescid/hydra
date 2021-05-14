package com.lambda_manager.utils;

// TODO: Maybe we need to rename LambdaTuple to appropriate name.
public class LambdaTuple<X, Y> {
    public final X function;
    public final Y lambda;
    public LambdaTuple(X function, Y lambda) {
        this.function = function;
        this.lambda = lambda;
    }
}
