package org.graalvm.argo.lambda_manager.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
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
                    return flowable.timeout(60, TimeUnit.SECONDS).blockingFirst();
                } catch (ReadTimeoutException readTimeoutException) {
                    Logger.log(Level.WARNING, "Received readTimeoutException in lambda " + lambda.getLambdaID() + ". Message: " + readTimeoutException.getMessage());
                    break;
                } catch (HttpClientException httpClientException) {
                    try {
                        Logger.log(Level.WARNING, "Received httpClientException in lambda " + lambda.getLambdaID() + ". Message: " + httpClientException.getMessage());
                        Thread.sleep(Configuration.argumentStorage.getHealthCheck());
                    } catch (InterruptedException interruptedException) {
                        Logger.log(Level.WARNING, "Received interruptedException");
                        // Skipping raised exception.
                    }
                }
            }
            lambda.setDecommissioned(true);
            Logger.log(Level.WARNING, Messages.HTTP_TIMEOUT);
            return Messages.HTTP_TIMEOUT;
        }
    }

    @Override
    public String registerFunction(Lambda lambda, Function function) {
        // TODO: optimization: read chunks of file and send it in parts.
        try (InputStream sourceFile = Files.newInputStream(function.buildFunctionSourceCodePath())) {
            String path = null;
            byte[] payload = canReuseCode(lambda) ? new byte[0] : sourceFile.readAllBytes();
            if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZING) {
                // The two optional parameters - GV sandbox and SVM ID.
                String sandbox = function.getGraalvisorSandbox() != null ? String.format("&sandbox=%s", function.getGraalvisorSandbox()) : "";
                String svmId = function.snapshotSandbox() ? String.format("&svmid=%s", function.getSvmId()) : "";
                final boolean binaryFunctionExecution = isBinaryFunctionExecution(lambda);
                path = String.format("/register?name=%s&language=%s&entryPoint=%s&isBinary=%s%s%s", getFunctionName(function, true), function.getLanguage().toString(), function.getEntryPoint(), binaryFunctionExecution, sandbox, svmId);
            } else if (lambda.getExecutionMode().isCustom()) {
                path = "/init";
            } else if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT || lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT) {
                path = String.format("/register?name=%s&language=%s&entryPoint=%s", function.getName(), function.getLanguage().toString(), function.getEntryPoint());
            } else if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALOS) {
                // Skip.
            } else {
                Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
            }
            if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALOS) {
                return "No registration needed in a GraalOS lambda.";
            } else {
                return sendRequest(HttpRequest.POST(path, payload), lambda);
            }
        } catch (IOException e) {
            Logger.log(Level.WARNING, String.format("Failed load function %s source file %s", function.getName(), function.buildFunctionSourceCodePath()));
            return Messages.ERROR_FUNCTION_UPLOAD;
        }
    }

    @Override
    public String deregisterFunction(Lambda lambda, Function function) {
        String path = null;
        String payload = null;

        if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZING) {
            path ="/deregister";
            payload = JsonUtils.convertParametersIntoJsonObject(null, null, getFunctionName(function, false));
        } else if (lambda.getExecutionMode().isCustom()) {
            Logger.log(Level.WARNING, String.format("Deregistering functions (%s) is not yet supported for custom runtimes!", function.getName()));
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
        }

        return sendRequest(HttpRequest.POST(path, payload), lambda);
    }

    @Override
    public String invokeFunction(Lambda lambda, Function function, String arguments) {
        // Only use the warmup endpoint if intend to do sandbox snapshotting.
        String path = function.snapshotSandbox() ? "/warmup?concurrency=1&requests=1" : "/";
        String payload = "";

        if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZING) {
            // Both canRebuild and readily-provided GV functions go here.
            payload = JsonUtils.convertParametersIntoJsonObject(arguments, null, getFunctionName(function, false), Configuration.argumentStorage.isDebugMode());
        } else if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT || lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT) {
            payload = JsonUtils.convertParametersIntoJsonObject(arguments, null, function.getName());
        } else if (lambda.getExecutionMode().isCustom()) {
            path = "/run";
            payload = "{ \"value\" : " + arguments + " }";
        } else if (lambda.getExecutionMode() == LambdaExecutionMode.GRAALOS) {
            path = "/helloworld";
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when invoking function %s!", lambda.getExecutionMode(), function.getName()));
        }

        return sendRequest(HttpRequest.POST(path, payload), lambda);
    }

    private static boolean isBinaryFunctionExecution(Lambda lambda){
                return lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED || lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZING;
            }

    private boolean canReuseCode(Lambda lambda) {
        return lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR && Configuration.argumentStorage.getLambdaType().isContainer();
    }

    /**
     * Should only be used for Graalvisor mode.
     * Graalvisor on Docker can copy function code from the filesystem
     * directly instead of reading it through POST requests.
     */
    private String getFunctionName(Function function, boolean url) {
        String functionName = function.getName();
        if (Configuration.argumentStorage.getLambdaType().isContainer()) {
            if (url) {
                // Escaping the slash character.
                return functionName + "%2F" + functionName;
            } else {
                return functionName + "/" + functionName;
            }
        } else {
            return functionName;
        }

    }
}
