package org.graalvm.argo.lambda_manager.client;

import org.graalvm.argo.lambda_manager.core.Lambda;

public interface LambdaManagerClient {
    String sendRequest(Lambda lambda);
}