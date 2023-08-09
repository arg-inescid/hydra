package org.graalvm.argo.lambda_manager.utils;

import io.micronaut.http.client.RxHttpClient;

public class LambdaConnection {
    public final String ip;
    public final String tap;
    public final RxHttpClient client;
    public final int port;

    public LambdaConnection(String ip, String tap, RxHttpClient client, int port) {
        this.ip = ip;
        this.tap = tap;
        this.client = client;
        this.port = port;
    }
}
