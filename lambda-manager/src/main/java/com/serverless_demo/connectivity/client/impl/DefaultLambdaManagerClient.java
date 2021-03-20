package com.serverless_demo.connectivity.client.impl;

import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.connectivity.client.LambdaManagerClient;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.exceptions.user.ErrorUploadingNewLambda;
import com.serverless_demo.utils.Tuple;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class DefaultLambdaManagerClient implements LambdaManagerClient {

    public void createNewClient(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration,
                                       boolean createLocalAddress) throws ErrorUploadingNewLambda {
        try {
            ArrayList<RxHttpClient> httpClients = lambda.list.getHttpClients();
            String lambdaName = lambda.list.getName();
            int lambdaId = lambda.instance.getId();

            if(createLocalAddress) {
                httpClients.add(RxHttpClient.create(new URL("http://localhost:" +
                        configuration.argumentStorage.getInstancePort(lambdaName, lambdaId))));
            } else {
                for (LambdaInstanceInfo availableInstance : lambda.list.getAvailableInstances()) {
                    httpClients.remove(availableInstance.getId());
                    httpClients.add(availableInstance.getId(), RxHttpClient.create(new URL(
                            configuration.argumentStorage.makeNewInstanceFullAddress(lambdaName))));
                }
            }
        } catch (MalformedURLException malformedURLException) {
            malformedURLException.printStackTrace(System.err);
            throw new ErrorUploadingNewLambda("Error during uploading new lambda [ " + lambda.list.getName() + " ]!", malformedURLException);
        }
    }

    public String sendRequest(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        String response;
        while(true) {
            try {
                response = lambda.list.getHttpClients().get(lambda.instance.getId()).retrieve(HttpRequest.GET("/")).blockingFirst();
                break;
            } catch (HttpClientException httpClientException) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(configuration.argumentStorage.getHealthCheck());
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace(System.err);
                }
            }
        }
        return response;
    }
}
