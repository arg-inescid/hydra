package com.lambda_manager.connectivity.client.impl;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.utils.LambdaTuple;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpParameters;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.reactivex.Flowable;

import java.util.logging.Level;

@SuppressWarnings("unused")
public class DefaultLambdaManagerClient implements LambdaManagerClient {

    private static final int FAULT_TOLERANCE = 300;

    @Override
    public HttpRequest<?> buildHTTPRequest(String parametersString) {
        MutableHttpRequest<Object> request = HttpRequest.GET("/");
        if(parametersString != null) {
            MutableHttpParameters requestParameters = request.getParameters();
            String[] parameters = parametersString.split(",");
            for(int i = 0; i < parameters.length; i++) {
                if(parameters[i].length() > 0) {
                    requestParameters.add(Integer.toString(i), parameters[i]);
                }
            }
        }
        return request;
    }

    @Override
    public String sendRequest(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        HttpRequest<?> request = buildHTTPRequest(lambda.lambda.getParameters());
        try (RxHttpClient client = lambda.lambda.getConnectionTriplet().client) {
            Flowable<String> flowable = client.retrieve(request);
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
            Logger.log(Level.WARNING, Messages.HTTP_TIMEOUT);
            return Messages.HTTP_TIMEOUT;
        }
    }
}
