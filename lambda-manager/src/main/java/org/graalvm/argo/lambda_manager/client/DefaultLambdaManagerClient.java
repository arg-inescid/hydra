package org.graalvm.argo.lambda_manager.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.Flowable;
import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.FunctionLanguage;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.JsonUtils;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

public class DefaultLambdaManagerClient implements LambdaManagerClient {

    private static final SplittableRandom RANDOM = new SplittableRandom();
    private static final int MAX_SLEEP_MS = 5000;
    private static final long REACTIVEX_TIMEOUT_SECONDS = 60;

    private String sendRequest(HttpRequest<?> request, Lambda lambda) {
        return sendRequest(request, lambda, REACTIVEX_TIMEOUT_SECONDS);
    }

    private String sendRequest(HttpRequest<?> request, Lambda lambda, long reactivexTimeout) {
        for (int failures = 0; failures < Configuration.argumentStorage.getFaultTolerance(); failures++) {
            try {
                Flowable<String> flowable = lambda.getConnection().client.retrieve(request);
                return flowable.timeout(reactivexTimeout, TimeUnit.SECONDS).blockingFirst();
            } catch (HttpClientException e) {
                if (e instanceof HttpClientResponseException) {
                    HttpClientResponseException responseException = (HttpClientResponseException) e;
                    if (responseException.getStatus() == HttpStatus.NOT_FOUND) {
                        // Response indicates 404 Not Found, no need to retry.
                        return responseException.getMessage();
                    }
                }
                Logger.log(Level.WARNING, "Received HttpClientException in lambda " + lambda.getLambdaID() + ". Message: " + e.getMessage());
                exponentialBackoff(failures);
            } catch (ReadTimeoutException e) {
                Logger.log(Level.WARNING, "Received ReadTimeoutException in lambda " + lambda.getLambdaID() + ". Message: " + e.getMessage());
                break;
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();
                if (cause instanceof TimeoutException) {
                    // Can be thrown if the ReactiveX timeout from Flowable has been reached.
                    Logger.log(Level.WARNING, "ReactiveX timeout in lambda " + lambda.getLambdaID() + ". Message: " + cause.getMessage());
                    exponentialBackoff(failures);
                } else {
                    // Some other exception that needs to be propagated.
                    throw e;
                }
            }
        }
        lambda.setDecommissioned(true);
        Logger.log(Level.WARNING, Messages.HTTP_TIMEOUT);
        return Messages.HTTP_TIMEOUT;
    }

    @Override
    public String registerFunction(Lambda lambda, Function function) {
            String path = null;
            byte[] payload = null;
            // Intentionally triggering 404 as a way to ensure that the webserver is up.
            sendRequest(lambda.getConnection().post("/ping", payload), lambda, 1);
            if (lambda.getExecutionMode().isHydra()) {
                // The two optional parameters - Hydra sandbox and SVM ID.
                String sandbox = function.getHydraSandbox() != null ? String.format("&sandbox=%s", function.getHydraSandbox()) : "";
                String svmId = function.snapshotSandbox() ? String.format("&svmid=%s", function.getSvmId()) : "";
                final boolean binaryFunctionExecution = isBinaryFunctionExecution(lambda);
                path = String.format("/register?name=%s&url=%s&language=%s&entryPoint=%s&isBinary=%s%s%s", function.getName(), function.getFunctionCode(), function.getLanguage().toString(), function.getEntryPoint(), binaryFunctionExecution, sandbox, svmId);
            } else if (lambda.getExecutionMode().isCustom()) {
                // TODO: optimization: read chunks of file and send it in parts.
                try (InputStream sourceFile = Files.newInputStream(function.buildFunctionSourceCodePath())) {
                    payload = sourceFile.readAllBytes();
                } catch (IOException e) {
                    Logger.log(Level.WARNING, String.format("Failed load function %s source file %s", function.getName(), function.buildFunctionSourceCodePath()));
                    return Messages.ERROR_FUNCTION_UPLOAD;
                }
                path = "/init";
            } else if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT || lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT) {
                path = String.format("/register?name=%s&url=%s&language=%s&entryPoint=%s", function.getName(), function.getFunctionCode(), function.getLanguage().toString(), function.getEntryPoint());
            } else if (lambda.getExecutionMode() == LambdaExecutionMode.KNATIVE) {
                return "No registration needed in a Knative lambda.";
            } else if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALOS) {
                return "No registration needed in a GraalOS lambda.";
            } else {
                Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
            }
            return sendRequest(lambda.getConnection().post(path, payload), lambda);
    }

    @Override
    public String deregisterFunction(Lambda lambda, Function function) {
        String path = null;
        String payload = null;

        if (lambda.getExecutionMode().isHydra()) {
            path ="/deregister";
            payload = JsonUtils.convertParametersIntoJsonObject(null, null, function.getName());
        } else if (lambda.getExecutionMode().isCustom()) {
            Logger.log(Level.WARNING, String.format("Deregistering functions (%s) is not yet supported for custom runtimes!", function.getName()));
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
        }

        return sendRequest(lambda.getConnection().post(path, payload), lambda);
    }

    @Override
    public String invokeFunction(Lambda lambda, Function function, String arguments) {
        // Only use the warmup endpoint if intend to do sandbox snapshotting.
        String path = function.snapshotSandbox() ? "/warmup?concurrency=1&requests=1" : "/";
        String payload = "";

        if (lambda.getExecutionMode().isHydra()) {
            // Both canRebuild and readily-provided Hydra functions go here.
            payload = JsonUtils.convertParametersIntoJsonObject(arguments, null, function.getName(), Configuration.argumentStorage.isDebugMode());
        } else if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT || lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT) {
            payload = JsonUtils.convertParametersIntoJsonObject(arguments, null, function.getName());
        } else if (lambda.getExecutionMode().isCustom()) {
            path = "/run";
            payload = "{ \"value\" : " + arguments + " }";
        } else if (lambda.getExecutionMode() == LambdaExecutionMode.KNATIVE) {
            payload = arguments;
        } else if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALOS) {
            path = "/helloworld";
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when invoking function %s!", lambda.getExecutionMode(), function.getName()));
        }
        return sendRequest(lambda.getConnection().post(path, payload), lambda);
    }

    private static boolean isBinaryFunctionExecution(Lambda lambda){
        return lambda.getExecutionMode() == LambdaExecutionMode.HYDRA_PGO || lambda.getExecutionMode() == LambdaExecutionMode.HYDRA_PGO_OPTIMIZED || lambda.getExecutionMode() == LambdaExecutionMode.HYDRA_PGO_OPTIMIZING;
    }

    private void exponentialBackoff(int failures) {
        try {
            // Exponential backoff with randomization element.
            int sleepTime = Configuration.argumentStorage.getHealthCheck() * (int) Math.pow(2, failures);
            sleepTime += RANDOM.nextInt(sleepTime);
            sleepTime = Math.min(sleepTime, MAX_SLEEP_MS);
            Thread.sleep(sleepTime);
        } catch (InterruptedException interruptedException) {
            // Skipping raised exception.
        }
    }
}
