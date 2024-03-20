package org.graalvm.argo.lambda_manager.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graalvm.argo.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import org.graalvm.argo.lambda_manager.exceptions.user.FunctionNotFound;
import org.graalvm.argo.lambda_manager.metrics.MetricsProvider;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.JsonUtils;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.utils.parser.ArgumentParser;
import io.micronaut.context.BeanContext;
import io.reactivex.Single;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LambdaManager {

    /**
     * This set contains all the lambdas tracked by the lambda manager.
     */
    public static final Set<Lambda> lambdas = Collections.newSetFromMap(new ConcurrentHashMap<Lambda, Boolean>());

    private static String formatRequestSpentTimeMessage(Lambda lambda, Function function, long spentTime, long infrTime) {
        String username = Configuration.coder.decodeUsername(function.getName());
        String functionName = Configuration.coder.decodeFunctionName(function.getName());
        return String.format(Messages.TIME_REQUEST, username, functionName, lambda.getExecutionMode(), lambda.getLambdaID(), spentTime, infrTime);
    }

    public static Single<String> processRequest(String username, String functionName, String arguments) {
        String response = null;
        Function function = null;
        Lambda lambda = null;
        long start = System.currentTimeMillis();

        if (!Configuration.isInitialized()) {
            Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
            return JsonUtils.constructJsonResponseObject(Messages.NO_CONFIGURATION_UPLOADED);
        }

        try {
            function = Configuration.storage.get(Configuration.coder.encodeFunctionName(username, functionName));
        } catch (FunctionNotFound functionNotFound) {
            Logger.log(Level.WARNING, functionNotFound.getMessage(), functionNotFound);
            return JsonUtils.constructJsonResponseObject(functionNotFound.getMessage());
        }
        LambdaExecutionMode targetMode = function.getLambdaExecutionMode();

        for (int i = 0; i < Configuration.LAMBDA_FAULT_TOLERANCE; i++) {
            try {
                lambda = Configuration.scheduler.schedule(function, targetMode);

                synchronized (lambda) {
                    if (lambda.isFunctionUploadRequired(function)) {
                        response = Configuration.client.registerFunction(lambda, function);
                        Logger.log(Level.FINE, String.format("Function %s registration in lambda %s returned %s", function.getName(), lambda.getLambdaName(), response));
                    }
                }

                long infrTime = System.currentTimeMillis() - start;

                response = Configuration.client.invokeFunction(lambda, function, arguments);

                // TODO: Change message returned from GuestAPI, check for this new message. Remember to keep HTTP_TIMEOUT branch.
                // This message should suggest that the Native Image runtime encountered unconfigured call.
                if (response.equals(Messages.HTTP_TIMEOUT)) {
                    if (function.canRebuild() && lambda.getExecutionMode() == LambdaExecutionMode.GRAALVISOR) {
                        // TODO: test fallback for GV once isolates do not terminate entire runtime
                        function.setStatus(FunctionStatus.NOT_BUILT_NOT_CONFIGURED);
                        targetMode = LambdaExecutionMode.HOTSPOT_W_AGENT;
                        Logger.log(Level.INFO, "Decommissioning (failed requests) lambda " + lambda.getLambdaID());
                        lambda.setDecommissioned(true);
                    }
                } else {
                    long requestTime = System.currentTimeMillis() - start;
                    MetricsProvider.addRequest();
                    Logger.log(Level.FINE, formatRequestSpentTimeMessage(lambda, function, requestTime, infrTime));
                    if (Configuration.argumentStorage.isDebugMode()) {
                        response += "; time spent in LM (seconds): " + requestTime / 1000.0;
                    }
                    break;
                }
            } catch (Throwable throwable) {
                if (Environment.notShutdownHookActive()) {
                    Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
                }
                response = Messages.INTERNAL_ERROR;
            } finally {
                if (lambda != null && function != null) {
                    Configuration.scheduler.reschedule(lambda, function);
                }
            }

        }

        return JsonUtils.constructJsonResponseObject(response);
    }

    public static Single<String> uploadFunction(String username,
                                                String functionName,
                                                String functionLanguage,
                                                String functionEntryPoint,
                                                String functionMemory,
                                                String functionRuntime,
                                                byte[] functionCode,
                                                boolean functionIsolation,
                                                boolean invocationCollocation,
                                                String gvSandbox) {
        String responseString;

        if (!Configuration.isInitialized()) {
            Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
            return JsonUtils.constructJsonResponseObject(Messages.NO_CONFIGURATION_UPLOADED);
        }

        try {
            String encodedFunctionName = Configuration.coder.encodeFunctionName(username, functionName);
            Function function = new Function(encodedFunctionName, functionLanguage, functionEntryPoint, functionMemory, functionRuntime, functionCode, functionIsolation, invocationCollocation, gvSandbox);
            Configuration.storage.register(encodedFunctionName, function, functionCode);
            Logger.log(Level.INFO, String.format(Messages.SUCCESS_FUNCTION_UPLOAD, functionName));
            responseString = String.format(Messages.SUCCESS_FUNCTION_UPLOAD, functionName);
        } catch (Exception e) {
            Logger.log(Level.SEVERE, String.format(Messages.ERROR_FUNCTION_UPLOAD, functionName), e);
            responseString = String.format(Messages.ERROR_FUNCTION_UPLOAD, functionName);
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            responseString = Messages.INTERNAL_ERROR;
        }
        return JsonUtils.constructJsonResponseObject(responseString);
    }

    public static Single<String> removeFunction(String username, String functionName) {
        String responseString;

        if (!Configuration.isInitialized()) {
            Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
            return JsonUtils.constructJsonResponseObject(Messages.NO_CONFIGURATION_UPLOADED);
        }

        try {
            // TODO - we also need to go to all lambdas and set their function to not registered.
            String encodeFunctionName = Configuration.coder.encodeFunctionName(username, functionName);
            Configuration.storage.unregister(encodeFunctionName);
            Logger.log(Level.INFO, String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName));
            responseString = String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName);
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            responseString = Messages.INTERNAL_ERROR;
        }
        return JsonUtils.constructJsonResponseObject(responseString);
    }

    public static Single<String> configureManager(String lambdaManagerConfiguration, BeanContext beanContext) {
        String responseString;
        try {
            if (!Configuration.isInitialized()) {
                ArgumentStorage.initializeLambdaManager(ArgumentParser.parse(lambdaManagerConfiguration), beanContext);
                Logger.log(Level.INFO, Messages.SUCCESS_CONFIGURATION_UPLOAD);
                responseString = Messages.SUCCESS_CONFIGURATION_UPLOAD;
            } else {
                responseString = Messages.CONFIGURATION_ALREADY_UPLOADED;
            }
        } catch (ErrorDuringParsingJSONFile e) {
            Logger.log(Level.SEVERE, e.getMessage(), e);
            responseString = Messages.ERROR_CONFIGURATION_UPLOAD;
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            responseString = Messages.INTERNAL_ERROR;
        }
        return JsonUtils.constructJsonResponseObject(responseString);
    }

    public static Single<String> getFunctions() {
        Object response;

        if (!Configuration.isInitialized()) {
            Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
            return JsonUtils.constructJsonResponseObject(Messages.NO_CONFIGURATION_UPLOADED);
        }

        try {
            Map<String, Function> functionsMap = Configuration.storage.getAll();
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode functionsArrayNode = mapper.createArrayNode();
            for (Map.Entry<String, Function> entry : functionsMap.entrySet()) {
                ObjectNode functionNode = mapper.createObjectNode();
                String username = Configuration.coder.decodeUsername(entry.getValue().getName());
                String functionName = Configuration.coder.decodeFunctionName(entry.getValue().getName());
                functionNode.put("user", username);
                functionNode.put("name", functionName);
                functionsArrayNode.add(functionNode);
            }
            response = functionsArrayNode;
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            response = Messages.INTERNAL_ERROR;
        }
        return JsonUtils.constructJsonResponseObject(response);
    }

}
