package org.graalvm.argo.lambda_manager.client;

import static org.graalvm.argo.lambda_manager.utils.JsonUtils.objectMapper;

import java.util.logging.Level;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.reactivex.Flowable;

@SuppressWarnings("unused")
public class DefaultLambdaManagerClient implements LambdaManagerClient {

    /**
     * Number of times a request will be re-sent to a particular Lambda upon an error.
     */
    private static final int FAULT_TOLERANCE = 300;

    private HttpRequest<?> buildHTTPRequest(Lambda lambda) {
        ObjectNode inputObject = objectMapper.createObjectNode();
        inputObject.put("arguments", lambda.getParameters());
        inputObject.put("name", lambda.getFunction().getEntryPoint());
        String argumentsJSON = "";
        try {
            argumentsJSON = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputObject);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return HttpRequest.<Object> POST("/", argumentsJSON);
    }

    @Override
    public String sendRequest(Lambda lambda) {
        HttpRequest<?> request = buildHTTPRequest(lambda);
        try (RxHttpClient client = lambda.getConnectionTriplet().client) {
            Flowable<String> flowable = client.retrieve(request);
            for (int failures = 0; failures < FAULT_TOLERANCE; failures++) {
                try {
                    return flowable.blockingFirst();
                } catch (ReadTimeoutException readTimeoutException) {
                    break;
                } catch (HttpClientException httpClientException) {
                    try {
                        Thread.sleep(Configuration.argumentStorage.getHealthCheck());
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
