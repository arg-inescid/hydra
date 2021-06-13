package com.lambda_manager.utils;

// TODO: Maybe we need to rename LambdaTuple to appropriate name.
// TODO - Why do we need a tuple? If a Lambda has a reference to its Function, then we could drop this LambdaTuple, right?
public class LambdaTuple<X, Y> {
    public final X function;
    public final Y lambda;
    public LambdaTuple(X function, Y lambda) {
        this.function = function;
        this.lambda = lambda;
    }
}
