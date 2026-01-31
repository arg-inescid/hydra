package org.graalvm.argo.lambda_manager.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.JsonUtils;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

public class DefaultLambdaManagerClient implements LambdaManagerClient {

    private static final long TIMEOUT_SECONDS = 60;

    private String sendRequest(String path, byte[] payload, Lambda lambda) {
        return sendRequest(path, payload, lambda, TIMEOUT_SECONDS);
    }

    private String sendRequest(String path, byte[] payload, Lambda lambda, long timeout) {
        return lambda.getConnection().sendRequest(path, payload, lambda, timeout);
    }

    @Override
    public String registerFunction(Lambda lambda, Function function) {
            LambdaExecutionMode mode = lambda.getExecutionMode();
            String path = null;
            byte[] payload = null;
            // For networked modes, intentionally triggering 404 as a way to ensure that the webserver is up.
            if (!mode.isGraalOS()) {
                sendRequest("/ping", payload, lambda, 1);
            }
            if (mode.isHydra()) {
                // The two optional parameters - Hydra sandbox and SVM ID.
                String sandbox = function.getHydraSandbox() != null ? String.format("&sandbox=%s", function.getHydraSandbox()) : "";
                String svmId = function.snapshotSandbox() ? String.format("&svmid=%s", function.getSvmId()) : "";
                final boolean binaryFunctionExecution = isBinaryFunctionExecution(lambda);
                path = String.format("/register?name=%s&url=%s&language=%s&entryPoint=%s&isBinary=%s%s%s", function.getName(), function.getFunctionCode(), function.getLanguage().toString(), function.getEntryPoint(), binaryFunctionExecution, sandbox, svmId);
            } else if (mode.isCustom()) {
                // TODO: optimization: read chunks of file and send it in parts.
                try (InputStream sourceFile = Files.newInputStream(function.buildFunctionSourceCodePath())) {
                    payload = sourceFile.readAllBytes();
                } catch (IOException e) {
                    Logger.log(Level.WARNING, String.format("Failed load function %s source file %s", function.getName(), function.buildFunctionSourceCodePath()));
                    return Messages.ERROR_FUNCTION_UPLOAD;
                }
                path = "/init";
            } else if (mode.isHotSpot()) {
                path = String.format("/register?name=%s&url=%s&language=%s&entryPoint=%s", function.getName(), function.getFunctionCode(), function.getLanguage().toString(), function.getEntryPoint());
            } else if (lambda.getExecutionMode() == LambdaExecutionMode.KNATIVE) {
                return "No registration needed in a Knative lambda.";
            } else if (mode.isGraalOS()) {
                path = "/command";
                // TODO - we need to keep the ep number rotating.
                payload = ("{ \"act\":\"add_ep\", \"ep\":9001, \"app\":\"" + function.getFunctionCode() + "\" }").getBytes();

            } else {
                Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when registering function %s!", lambda.getExecutionMode(), function.getName()));
            }
            return sendRequest(path, payload, lambda);
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

        return sendRequest(path, payload.getBytes(), lambda);
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
            path = "/command";
            payload = "{ \"act\":\"add_isolate\", \"ep\":9001 }";
        } else {
            Logger.log(Level.WARNING, String.format("Unexpected lambda mode (%s) when invoking function %s!", lambda.getExecutionMode(), function.getName()));
        }
        return sendRequest(path, payload.getBytes(), lambda);
    }

    private static boolean isBinaryFunctionExecution(Lambda lambda){
        return  lambda.getExecutionMode() == LambdaExecutionMode.HYDRA_PGO ||
                lambda.getExecutionMode() == LambdaExecutionMode.HYDRA_PGO_OPTIMIZED ||
                lambda.getExecutionMode() == LambdaExecutionMode.HYDRA_PGO_OPTIMIZING;
    }
}
