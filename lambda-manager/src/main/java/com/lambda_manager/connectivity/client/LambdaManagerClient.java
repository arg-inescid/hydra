package com.lambda_manager.connectivity.client;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.HttpRequest;

public interface LambdaManagerClient {
    HttpRequest<?> buildHTTPRequest(String parameters);
    String sendRequest(LambdaTuple<Function, Lambda> lambda);
}