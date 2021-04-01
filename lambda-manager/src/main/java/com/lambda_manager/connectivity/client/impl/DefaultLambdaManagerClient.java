package com.lambda_manager.connectivity.client.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.utils.Tuple;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;

import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings("unused")
public class DefaultLambdaManagerClient implements LambdaManagerClient {

    public void createNewClient(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                                LambdaManagerConfiguration configuration) throws ErrorUploadingNewLambda {
        try {
            String ip = lambda.instance.getIp();
            int port = configuration.argumentStorage.getNextPort();
            int lambdaPort = configuration.argumentStorage.getLambdaPort();

            lambda.instance.setPort(port);
            lambda.instance.setLocalClient(RxHttpClient.create(new URL("http://localhost:" + port)));
            lambda.instance.setPublicClient(RxHttpClient.create(new URL("http://" + ip + ":" + lambdaPort)));
        } catch (MalformedURLException malformedURLException) {
            throw new ErrorUploadingNewLambda("Error during uploading new lambda [" + lambda.list.getName() + "]!",
                    malformedURLException);
        }
    }

    public String sendRequest(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        String response;
        while(true) {
            try {
                if (lambda.list.getStatus() != LambdaStatusType.BUILT) {
                    response = lambda.instance.getLocalClient().retrieve(HttpRequest.GET("/")).blockingFirst();
                } else {
                    response = lambda.instance.getPublicClient().retrieve(HttpRequest.GET("/")).blockingFirst();
                }
                break;
            } catch (HttpClientException httpClientException) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(configuration.argumentStorage.getHealthCheck());
                } catch (InterruptedException interruptedException) {
                    // Skipping raised exception.
                }
            }
        }
        return response;
    }
}
