package com.lambda_manager.collectors.function_storage.impl;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.collectors.function_storage.FunctionStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class DefaultFunctionStorage implements FunctionStorage {

    private final ConcurrentHashMap<String, Function> functions = new ConcurrentHashMap<>();

    @Override
    public Function register(String functionName) {
        Function function = functions.get(functionName);
        if(function == null) {
            function = new Function(functionName);
            functions.put(functionName, function);
        }
        return function;
    }

    @Override
    public void unregister(String functionName) {
        functions.remove(functionName);
    }

    @Override
    public Function get(String functionName) throws FunctionNotFound {
        Function function = functions.get(functionName);
        if (function == null) {
            throw new FunctionNotFound(String.format(Messages.FUNCTION_NOT_FOUND, functionName));
        }
        return function;
    }

    @Override
    public Map<String, Function> getAll() {
        return functions;
    }
}
