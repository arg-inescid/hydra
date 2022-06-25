package org.graalvm.argo.lambda_manager.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.FunctionLanguage;
import org.graalvm.argo.lambda_manager.utils.JsonUtils;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.reactivex.Flowable;

public class DefaultLambdaManagerClient implements LambdaManagerClient {

    private String sendRequest(HttpRequest<?> request, Lambda lambda) {
        try (RxHttpClient client = lambda.getConnectionTriplet().client) {
            Flowable<String> flowable = client.retrieve(request);
            for (int failures = 0; failures < Configuration.FAULT_TOLERANCE; failures++) {
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

    @Override
    public String registerFunction(Lambda lambda, Function function) {
        String path = String.format("/register?name=%s&language=%s&entryPoint=%s",
                function.getName(),
                function.getLanguage().toString(),
                function.getEntryPoint());

        // TODO: optimization: read chunks of file and send it in parts.
        try (InputStream sourceFile = Files.newInputStream(function.buildFunctionSourceCodePath())) {
            return sendRequest(HttpRequest.POST(path, sourceFile.readAllBytes()), lambda);
        } catch (IOException e) {
            e.printStackTrace();
            return Messages.ERROR_FUNCTION_UPLOAD;
        }
    }

    @Override
    public String deregisterFunction(Lambda lambda, Function function) {
        String path ="/deregister";
        String argumentsJSON = JsonUtils.convertParametersIntoJsonObject(null, null, function.getName());
        return sendRequest(HttpRequest.POST(path, argumentsJSON), lambda);
    }

    @Override
    public String invokeFunction(Lambda lambda, Function function, String arguments) {
        String path ="/";
        String argumentsJSON = "";

        if (function.getLanguage() == FunctionLanguage.NATIVE_JAVA) {
            argumentsJSON = JsonUtils.convertParametersIntoJsonObject(arguments, null, function.getEntryPoint());
        } else {
            argumentsJSON = JsonUtils.convertParametersIntoJsonObject(arguments, null, function.getName());
        }

        return sendRequest(HttpRequest.POST(path, argumentsJSON), lambda);
    }
}
