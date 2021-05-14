package com.lambda_manager.connectivity.client.impl;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.utils.LambdaTuple;
import com.lambda_manager.utils.Messages;
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

    public String sendRequest(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        try (RxHttpClient client = lambda.lambda.getConnectionTriplet().client) {
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
            logger.log(Level.WARNING, Messages.HTTP_TIMEOUT);
            return Messages.HTTP_TIMEOUT;
        }
    }
}
