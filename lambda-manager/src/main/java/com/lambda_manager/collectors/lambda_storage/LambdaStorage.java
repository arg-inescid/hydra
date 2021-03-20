package com.lambda_manager.collectors.lambda_storage;

import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;

import java.util.Map;

public interface LambdaStorage {
    LambdaInstancesInfo register(String lambdaName);
    void unregister(String lambdaName);
    LambdaInstancesInfo get(String lambdaName);
    Map<String, LambdaInstancesInfo> getAll();
}
