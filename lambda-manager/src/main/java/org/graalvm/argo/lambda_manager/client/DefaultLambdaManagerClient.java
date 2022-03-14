package org.graalvm.argo.lambda_manager.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.JsonUtils;
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

    private Object buildHTTPRequestArguments(Lambda lambda) {
        Function function = lambda.getFunction();
        String functionName = function.getName();
        String entryPoint = function.getEntryPoint();
        String arguments = lambda.getArguments();

        switch (lambda.getTruffleStatus()) {
            case NEED_REGISTRATION:
                // TODO: optimization: read chunks of file and send it in parts.
                try (InputStream sourceFile = Files.newInputStream(function.buildFunctionSourceCodePath())) {
                    return sourceFile.readAllBytes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            case READY_FOR_EXECUTION:
                return JsonUtils.convertParametersIntoJsonObject(arguments, null, functionName);
            case DEREGISTER:
                return JsonUtils.convertParametersIntoJsonObject(null, null, functionName);
            case NOT_TRUFFLE_LANG:
                return JsonUtils.convertParametersIntoJsonObject(arguments, null, entryPoint);
            default:
                throw new IllegalStateException("Unexpected value: " + lambda.getTruffleStatus());
        }
    }

    private String buildHTTPRequestPath(Lambda lambda) {
        switch (lambda.getTruffleStatus()) {
            case NEED_REGISTRATION:
                Function function = lambda.getFunction();
                String functionName = function.getName();
                String entryPoint = function.getEntryPoint();
                String arguments = lambda.getArguments();
                return String.format("/register?name=%s&language=%s&entryPoint=%s", functionName, function.getLanguage().toString(), entryPoint);
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
        Object argumentsJSON = buildHTTPRequestArguments(lambda);
        if (argumentsJSON == null) {
            argumentsJSON = "";
        }
        return HttpRequest.POST(buildHTTPRequestPath(lambda), argumentsJSON);
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
