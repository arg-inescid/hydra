package org.graalvm.argo.lambda_manager.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
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
        try (RxHttpClient client = lambda.getConnection().client) {
            Flowable<String> flowable = client.retrieve(request);
            for (int failures = 0; failures < Configuration.FAULT_TOLERANCE; failures++) {
                try {
                    return flowable.blockingFirst();
                } catch (ReadTimeoutException readTimeoutException) {
                    Logger.log(Level.WARNING, "Received readTimeoutException");
                    break;
                } catch (HttpClientException httpClientException) {
                    try {
                        Logger.log(Level.WARNING, "Received httpClientException");
                        Thread.sleep(Configuration.argumentStorage.getHealthCheck());
                    } catch (InterruptedException interruptedException) {
                        Logger.log(Level.WARNING, "Received interruptedException");
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
        // TODO: optimization: read chunks of file and send it in parts.
        try (InputStream sourceFile = Files.newInputStream(function.buildFunctionSourceCodePath())) {
            String path = null;
            if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR) {
                String sandbox = function.getGraalvisorSandbox();
                if (sandbox != null) {
                    path = String.format("/register?name=%s&language=%s&entryPoint=%s&sandbox=%s", function.getName(), function.getLanguage().toString(), function.getEntryPoint(), sandbox);
                } else {
                    path = String.format("/register?name=%s&language=%s&entryPoint=%s", function.getName(), function.getLanguage().toString(), function.getEntryPoint());
                }
            } else if (lambda.getExecutionMode() == LambdaExecutionMode.CUSTOM) {
                path = "/init";
            } else if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT || lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT) {
                path = String.format("/register?name=%s&language=%s&entryPoint=%s", function.getName(), function.getLanguage().toString(), function.getEntryPoint());
            } else {
                Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
            }
            return sendRequest(HttpRequest.POST(path, sourceFile.readAllBytes()), lambda);
        } catch (IOException e) {
            Logger.log(Level.WARNING, String.format("Failed load function %s source file %s", function.getName(), function.buildFunctionSourceCodePath()));
            return Messages.ERROR_FUNCTION_UPLOAD;
        }
    }

    @Override
    public String deregisterFunction(Lambda lambda, Function function) {
        String path = null;
        String payload = null;

        if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR) {
            path ="/deregister";
            payload = JsonUtils.convertParametersIntoJsonObject(null, null, function.getName());
        } else if (lambda.getExecutionMode() == LambdaExecutionMode.CUSTOM) {
            Logger.log(Level.WARNING, String.format("Deregistering functions (%s) is not yet supported for custom runtimes!", function.getName()));
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
        }

        return sendRequest(HttpRequest.POST(path, payload), lambda);
    }

    @Override
    public String invokeFunction(Lambda lambda, Function function, String arguments) {
        String path ="/";
        String payload = "";

        if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT || lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT) {
            payload = JsonUtils.convertParametersIntoJsonObject(arguments, null, function.getName());
        } else if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR) {
            // Both canRebuild and readily-provided GV functions go here
            payload = JsonUtils.convertParametersIntoJsonObject(arguments, null, function.getName(), Configuration.argumentStorage.isDebugMode());
        } else if (lambda.getExecutionMode() == LambdaExecutionMode.CUSTOM) {
            path = "/run";
            if (function.getLanguage() == FunctionLanguage.JAVA) {
                payload = "{ \"value\" : " + arguments + " }";
            } else {
                payload = "{ }"; // TODO - receive from arguments
            }
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when invoking function %s!", lambda.getExecutionMode(), function.getName()));
        }

        return sendRequest(HttpRequest.POST(path, payload), lambda);
    }
}
