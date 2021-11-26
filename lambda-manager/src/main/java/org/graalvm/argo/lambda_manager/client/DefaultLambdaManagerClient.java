package org.graalvm.argo.lambda_manager.client;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.reactivex.Flowable;
import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.JsonUtils;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class DefaultLambdaManagerClient implements LambdaManagerClient {

    /**
     * Number of times a request will be re-sent to a particular Lambda upon an error.
     */
    private static final int FAULT_TOLERANCE = 300;

    private String buildHTTPRequestArguments(Lambda lambda) {
        Function function = lambda.getFunction();
        String functionName = function.getName();
        String entryPoint = function.getEntryPoint();
        String arguments = lambda.getArguments();

        switch (lambda.getTruffleStatus()) {
            case NEED_REGISTRATION:
                try {
                    return JsonUtils.convertParametersIntoJsonObject(arguments, entryPoint, functionName,
                            Files.readString(function.buildFunctionSourceCodePath()), function.getLanguage().toString());
                } catch (IOException e) {
                    // TODO: Handle this error!
                    e.printStackTrace();
                }
            case READY_FOR_EXECUTION:
                return JsonUtils.convertParametersIntoJsonObject(arguments, null, functionName, null, null);
            case DEREGISTER:
                return JsonUtils.convertParametersIntoJsonObject(null, null, functionName, null, null);
            case NOT_TRUFFLE_LANG:
                return JsonUtils.convertParametersIntoJsonObject(arguments, null, entryPoint,null, null);
            default:
                throw new IllegalStateException("Unexpected value: " + lambda.getTruffleStatus());
        }
    }

    private String buildHTTPRequestPath(Lambda lambda) {
        switch (lambda.getTruffleStatus()) {
            case NEED_REGISTRATION:
                return "/register";
            case READY_FOR_EXECUTION:
            case NOT_TRUFFLE_LANG:
                return "/";
            case DEREGISTER:
                return "/deregister";
            default:
                throw new IllegalStateException("Unexpected value: " + lambda.getTruffleStatus());
        }
    }

    private HttpRequest<?> buildHTTPRequest(Lambda lambda) {
        String argumentsJSON = buildHTTPRequestArguments(lambda);
        if (argumentsJSON == null) {
            argumentsJSON = "";
        }
        return HttpRequest.<Object>POST(buildHTTPRequestPath(lambda), argumentsJSON);
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
