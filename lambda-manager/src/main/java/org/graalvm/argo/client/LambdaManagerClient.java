package com.lambda_manager.client;

import com.lambda_manager.core.Lambda;

public interface LambdaManagerClient {
    String sendRequest(Lambda lambda);
}