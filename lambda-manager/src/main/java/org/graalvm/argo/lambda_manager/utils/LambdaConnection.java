package org.graalvm.argo.lambda_manager.utils;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.netty.DefaultHttpClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class LambdaConnection {
    public final String ip;
    public final String tap;
    public final RxHttpClient client;
    public final int port;

    public LambdaConnection(String ip, String tap, int port) throws MalformedURLException {
        this.ip = ip;
        this.tap = tap;
        URL url = new URL("http", ip, port, "/");
        HttpClientConfiguration config = new DefaultHttpClientConfiguration();
        config.setReadTimeout(Duration.ofSeconds(60));
        this.client = new DefaultHttpClient(url, config);
        this.port = port;
    }

    // Keeping two versions instead of using Java Generics for performance reasons.
    public HttpRequest<byte[]> post(String path, byte[] payload) {
        return HttpRequest.POST(path, payload);
    }

    public HttpRequest<String> post(String path, String payload) {
        return HttpRequest.POST(path, payload);
    }
}
