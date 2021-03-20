package com.serverless_demo.collectors.lambda_storage;

import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;

import java.util.Map;

public interface LambdaStorage {
    LambdaInstancesInfo register(String lambdaName);
    void unregister(String lambdaName);
    LambdaInstancesInfo get(String lambdaName);
    Map<String, LambdaInstancesInfo> getAll();
}
