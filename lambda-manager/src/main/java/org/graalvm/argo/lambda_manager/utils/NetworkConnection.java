package org.graalvm.argo.lambda_manager.utils;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.Flowable;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class NetworkConnection implements LambdaConnection {
    private final String ip;
    private final int port;
    private final String tap;
    private final RxHttpClient client;

    private static final SplittableRandom RANDOM = new SplittableRandom();
    private static final int MAX_SLEEP_MS = 5000;

    public NetworkConnection(String ip, String tap, int port) throws MalformedURLException {
        this.ip = ip;
        this.tap = tap;
        URL url = new URL("http", ip, port, "/");
        HttpClientConfiguration config = new DefaultHttpClientConfiguration();
        config.setReadTimeout(Duration.ofSeconds(30));
        this.client = new DefaultHttpClient(url, config);
        this.port = port;
    }

    @Override
    public boolean waitUntilReady() {
        return NetworkUtils.waitForOpenPort(this.ip, this.port, 500);
    }

    @Override
    public String sendRequest(String path, byte[] payload, Lambda lambda, long timeout) {
        HttpRequest<?> request = post(path, payload);
        for (int failures = 0; failures < Configuration.argumentStorage.getFaultTolerance(); failures++) {
            try {
                Flowable<String> flowable = this.client.retrieve(request);
                return flowable.timeout(timeout, TimeUnit.SECONDS).blockingFirst();
            } catch (HttpClientException e) {
                if (e instanceof HttpClientResponseException) {
                    HttpClientResponseException responseException = (HttpClientResponseException) e;
                    if (responseException.getStatus() == HttpStatus.NOT_FOUND) {
                        // Response indicates 404 Not Found, no need to retry.
                        return responseException.getMessage();
                    }
                }
                Logger.log(Level.WARNING, "Received HttpClientException in lambda " + lambda.getLambdaID() + ". Message: " + e.getMessage());
                exponentialBackoff(failures);
            } catch (ReadTimeoutException e) {
                Logger.log(Level.WARNING, "Received ReadTimeoutException in lambda " + lambda.getLambdaID() + ". Message: " + e.getMessage());
                break;
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();
                if (cause instanceof TimeoutException) {
                    // Can be thrown if the ReactiveX timeout from Flowable has been reached.
                    Logger.log(Level.WARNING, "ReactiveX timeout in lambda " + lambda.getLambdaID() + ". Message: " + cause.getMessage());
                    exponentialBackoff(failures);
                } else {
                    // Some other exception that needs to be propagated.
                    throw e;
                }
            }
        }
        lambda.setDecommissioned(true);
        Logger.log(Level.WARNING, Messages.HTTP_TIMEOUT);
        return Messages.HTTP_TIMEOUT;
    }

    @Override
    public void close() {
        this.client.close();
    }

    private HttpRequest<byte[]> post(String path, byte[] payload) {
        return HttpRequest.POST(path, payload == null ? new byte[0] : payload);
    }

    private void exponentialBackoff(int failures) {
        try {
            // Exponential backoff with randomization element.
            int sleepTime = Configuration.argumentStorage.getHealthCheck() * (int) Math.pow(2, failures);
            sleepTime += RANDOM.nextInt(sleepTime);
            sleepTime = Math.min(sleepTime, MAX_SLEEP_MS);
            Thread.sleep(sleepTime);
        } catch (InterruptedException interruptedException) {
            // Skipping raised exception.
        }
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getTap() {
        return tap;
    }

    public RxHttpClient getClient() {
        return client;
    }
}
