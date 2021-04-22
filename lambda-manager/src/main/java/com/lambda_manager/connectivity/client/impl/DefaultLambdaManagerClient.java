package com.lambda_manager.connectivity.client.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.reactivex.Flowable;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class DefaultLambdaManagerClient implements LambdaManagerClient {

    private static final int FAULT_TOLERANCE = 300;

    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public String sendRequest(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        try (RxHttpClient client = lambda.instance.getConnectionTriplet().client) {
            Flowable<String> flowable = client.retrieve(HttpRequest.GET("/"));
            for (int failures = 0; failures < FAULT_TOLERANCE; failures++) {
                try {
                    return flowable.blockingFirst();
                } catch (HttpClientException httpClientException) {
                    try {
                        Thread.sleep(configuration.argumentStorage.getHealthCheck());
                    } catch (InterruptedException interruptedException) {
                        // Skipping raised exception.
                    }
                }
            }
            logger.log(Level.WARNING, "HTTP request timeout!");
            return "HTTP request timeout!";
        }
    }
}
