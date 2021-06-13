package com.lambda_manager.collectors.function_storage;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.exceptions.user.FunctionNotFound;

import java.util.Map;

public interface FunctionStorage {
    Function register(String functionName);
    void unregister(String functionName);
    Function get(String functionName) throws FunctionNotFound;
    Map<String, Function> getAll();
}
