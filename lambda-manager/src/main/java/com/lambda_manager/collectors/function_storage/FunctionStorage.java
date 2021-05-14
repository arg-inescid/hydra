package com.lambda_manager.collectors.function_storage;

import com.lambda_manager.collectors.meta_info.Function;

import java.util.Map;

public interface FunctionStorage {
    Function register(String functionName);
    void unregister(String functionName);
    Function get(String functionName);
    Map<String, Function> getAll();
}
