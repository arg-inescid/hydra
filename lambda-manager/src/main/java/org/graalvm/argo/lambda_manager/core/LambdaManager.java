package org.graalvm.argo.lambda_manager.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graalvm.argo.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import org.graalvm.argo.lambda_manager.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import org.graalvm.argo.lambda_manager.exceptions.user.ErrorDuringCreatingConnectionPool;
import org.graalvm.argo.lambda_manager.exceptions.user.FunctionNotFound;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.JsonUtils;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.MetricsProvider;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.utils.parser.ArgumentParser;
import io.micronaut.context.BeanContext;
import io.reactivex.Single;

import java.util.Map;
import java.util.logging.Level;

public class LambdaManager {

    private static String formatRequestSpentTimeMessage(Lambda lambda, Function function, long spentTime) {
        String username = Configuration.coder.decodeUsername(function.getName());
        String functionName = Configuration.coder.decodeFunctionName(function.getName());
        switch (lambda.getExecutionMode()) {
            case HOTSPOT_W_AGENT:
                return String.format(Messages.TIME_HOTSPOT_W_AGENT, username, functionName, lambda.getProcess().pid(), spentTime);
            case HOTSPOT:
                return String.format(Messages.TIME_HOTSPOT, username, functionName, lambda.getProcess().pid(), spentTime);
            case NATIVE_IMAGE:
                return String.format(Messages.TIME_NATIVE_IMAGE, username, functionName, lambda.getProcess().pid(), spentTime);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static Single<String> processRequest(String username, String functionName, String arguments) {
        String response = null;

        if (!Configuration.isInitialized()) {
            Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
            return JsonUtils.constructJsonResponseObject(Messages.NO_CONFIGURATION_UPLOADED);
        }

        try {
            Function function = Configuration.storage.get(Configuration.coder.encodeFunctionName(username, functionName));
            LambdaExecutionMode targetMode = null;

            for (int i = 0; i < Configuration.LAMBDA_FAULT_TOLERANCE; i++) {
                long start = System.currentTimeMillis();
                Lambda lambda = Configuration.scheduler.schedule(function, targetMode);

                if (!lambda.isRegisteredInLambda(function)) {
                    // TODO - check if concurrency could be a problem on the lambda side.
                    Configuration.client.registerFunction(lambda, function);
                    lambda.setRegisteredInLambda(function);
                }

                response = Configuration.client.invokeFunction(lambda, function, arguments);
                Configuration.scheduler.reschedule(lambda, function);

                if (response.equals(Messages.HTTP_TIMEOUT)) {
                    if (lambda.getExecutionMode() == LambdaExecutionMode.NATIVE_IMAGE) {
                        function.setStatus(FunctionStatus.NOT_BUILT_NOT_CONFIGURED);
                        targetMode = LambdaExecutionMode.HOTSPOT_W_AGENT;
                    }
                    Logger.log(Level.INFO, "Decommissioning (failed requests) lambda " + lambda.getProcess().pid());
                    function.decommissionLambda(lambda);
                } else {
                    long spentTime = System.currentTimeMillis() - start;
                    MetricsProvider.reportRequestTime(spentTime);
                    Logger.log(Level.FINE, formatRequestSpentTimeMessage(lambda, function, spentTime));
                    break;
                }
            }

        } catch (FunctionNotFound functionNotFound) {
            Logger.log(Level.WARNING, functionNotFound.getMessage(), functionNotFound);
            response = functionNotFound.getMessage();
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            response = Messages.INTERNAL_ERROR;
        }
        return JsonUtils.constructJsonResponseObject(response);
    }

    public static Single<String> uploadFunction(String username,
                                                String functionName,
                                                String functionLanguage,
                                                String functionEntryPoint,
                                                String functionMemory,
                                                String functionRuntime,
                                                byte[] functionCode) {
        String responseString;

        if (!Configuration.isInitialized()) {
            Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
            return JsonUtils.constructJsonResponseObject(Messages.NO_CONFIGURATION_UPLOADED);
        }

        try {
            String encodeFunctionName = Configuration.coder.encodeFunctionName(username, functionName);
            Function function = new Function(encodeFunctionName, functionLanguage, functionEntryPoint, functionMemory, functionRuntime);
            Configuration.storage.register(encodeFunctionName, function, functionCode);
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
        } catch (ErrorDuringParsingJSONFile | ErrorDuringReflectiveClassCreation | ErrorDuringCreatingConnectionPool e) {
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
                functionNode.put("maxLambdas", entry.getValue().getTotalNumberLambdas());
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
