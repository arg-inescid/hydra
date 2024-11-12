package org.graalvm.argo.lambda_manager.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

public class LambdaConnection {
    public final String ip;
    public final String tap;
    public final HttpClient client;
    public final int port;

    private final String address;

    public LambdaConnection(String ip, String tap, int port) {
        this.ip = ip;
        this.tap = tap;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(60)).build();
        this.port = port;
        this.address = String.join("", "http://", ip, ":", Integer.toString(port));
    }

    public HttpRequest post(String path, byte[] payload) {
        return HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(60))
                .uri(URI.create(address.concat(path)))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
    }

    public HttpRequest post(String path, String payload) {
        return HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(60))
                .uri(URI.create(address.concat(path)))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
    }
}
