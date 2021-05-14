package com.lambda_manager.connectivity.client;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.utils.LambdaTuple;

public interface LambdaManagerClient {
    String sendRequest(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration);
}