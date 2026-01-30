package org.graalvm.argo.lambda_manager.utils;

import org.graalvm.argo.lambda_manager.core.Lambda;

public interface LambdaConnection {

    public boolean waitUntilReady();

    public String sendRequest(String path, byte[] payload, Lambda lambda, long timeout);

    public void close();
}
