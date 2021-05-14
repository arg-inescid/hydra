package com.lambda_manager.collectors.function_storage.impl;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.function_storage.FunctionStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class DefaultFunctionStorage implements FunctionStorage {

    private final ConcurrentHashMap<String, Function> lambdas = new ConcurrentHashMap<>();

    @Override
    public Function register(String functionName) {
        Function function = lambdas.get(functionName);
        if(function == null) {
            function = new Function(functionName);
            lambdas.put(functionName, function);
        }
        return function;
    }

    @Override
    public void unregister(String functionName) {
        lambdas.remove(functionName);
    }

    @Override
    public Function get(String functionName) {
        return lambdas.get(functionName);
    }

    @Override
    public Map<String, Function> getAll() {
        return lambdas;
    }
}
