package com.lambda_manager.connectivity.client;

import com.lambda_manager.collectors.meta_info.Lambda;

public interface LambdaManagerClient {
    String sendRequest(Lambda lambda);
}