package com.lambda_manager.connectivity.client.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.utils.Tuple;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class DefaultLambdaManagerClient implements LambdaManagerClient {

	private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private final int failThreshold = 600;

    public void createNewClient(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                                LambdaManagerConfiguration configuration) throws ErrorUploadingNewLambda {
        try {
            String ip = lambda.instance.getIp();
            int port = configuration.argumentStorage.getNextPort();
            int lambdaPort = configuration.argumentStorage.getLambdaPort();

            lambda.instance.setPort(port);
            lambda.instance.setPublicClient(RxHttpClient.create(new URL("http://" + ip + ":" + lambdaPort)));
        } catch (MalformedURLException malformedURLException) {
            throw new ErrorUploadingNewLambda("Error during uploading new lambda [" + lambda.list.getName() + "]!",
                    malformedURLException);
        }
    }

    public String sendRequest(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        for (int failures = 0; failures < failThreshold; failures++) {
            try {
                return lambda.instance.getPublicClient().retrieve(HttpRequest.GET("/")).blockingFirst();
            } catch (HttpClientException httpClientException) {
                try {
                    Thread.sleep(configuration.argumentStorage.getHealthCheck());
                } catch (InterruptedException interruptedException) {
                    // Skipping raised exception.
                }
            }
        }
        logger.log(Level.WARNING, "HTTP Client Exception request timedout");
        return "ERROR: request timedout.";
    }
}
