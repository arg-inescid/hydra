package com.lambda_manager.collectors.lambda_storage.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.collectors.lambda_storage.LambdaStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class DefaultLambdaStorage implements LambdaStorage {

    private final ConcurrentHashMap<String, LambdaInstancesInfo> lambdas = new ConcurrentHashMap<>();

    @Override
    public LambdaInstancesInfo register(String lambdaName) {
        LambdaInstancesInfo lambdaInstancesInfo = lambdas.get(lambdaName);
        if(lambdaInstancesInfo == null) {
            lambdaInstancesInfo = new LambdaInstancesInfo(lambdaName);
            lambdas.put(lambdaName, lambdaInstancesInfo);
        }
        return lambdaInstancesInfo;
    }

    @Override
    public void unregister(String lambdaName) {
        lambdas.remove(lambdaName);
    }

    @Override
    public LambdaInstancesInfo get(String lambdaName) {
        return lambdas.get(lambdaName);
    }

    @Override
    public Map<String, LambdaInstancesInfo> getAll() {
        return lambdas;
    }
}
