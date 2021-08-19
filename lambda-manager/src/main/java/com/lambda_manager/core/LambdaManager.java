package com.lambda_manager.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import com.lambda_manager.exceptions.user.ErrorDuringCreatingConnectionPool;
import com.lambda_manager.exceptions.user.ErrorUploadingLambda;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.utils.JsonUtils;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;
import com.lambda_manager.utils.parser.ArgumentParser;
import io.micronaut.context.BeanContext;
import io.reactivex.Single;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class LambdaManager {

	/** Number of times a request will be sent to a different Lambda upon timeout. */
	// TODO - This value should be configurable.
	private static final int LAMBDA_FAULT_TOLERANCE = 3;

    private LambdaManager() {
    }

    private static String formatRequestSpentTimeMessage(Lambda lambda, long spentTime) {
        String username = Configuration.coder.decodeUsername(lambda.getFunction().getName());
        String functionName = Configuration.coder.decodeFunctionName(lambda.getFunction().getName());
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

    public static Single<String> processRequest(String username, String functionName, String parameters) {
        String responseString = null;
        try {
            if (!Configuration.isInitialized()) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                responseString = Messages.NO_CONFIGURATION_UPLOADED;
            }

            Function function = Configuration.storage.get(Configuration.coder.encode(username, functionName));
            String response = null;
            LambdaExecutionMode targetMode = null;

            // TODO - We should strive to have a simple LambdaManager. This loop should be part of the scheduler?
            for (int i = 0; i < LAMBDA_FAULT_TOLERANCE; i++) {
                long start = System.currentTimeMillis();
                Lambda lambda = Configuration.scheduler.schedule(function, targetMode);
                lambda.setParameters(parameters);
                response = Configuration.client.sendRequest(lambda);
                Configuration.optimizer.registerCall(lambda);
                Configuration.scheduler.reschedule(lambda);

                if (response.equals(Messages.HTTP_TIMEOUT)) {
                    if (lambda.getExecutionMode() == LambdaExecutionMode.NATIVE_IMAGE) {
                        function.setStatus(FunctionStatus.NOT_BUILT_NOT_CONFIGURED);
                        targetMode = LambdaExecutionMode.HOTSPOT_W_AGENT;
                    }
                    Logger.log(Level.INFO, "Decommisioning (failed requests) lambda " + lambda.getProcess().pid());
                    lambda.getFunction().decommissionLambda(lambda);
                } else {
                    Logger.log(Level.FINE, formatRequestSpentTimeMessage(lambda, System.currentTimeMillis() - start));
                    break;
                }
            }

            responseString = response;
        } catch (FunctionNotFound functionNotFound) {
            Logger.log(Level.WARNING, functionNotFound.getMessage(), functionNotFound);
            responseString = functionNotFound.getMessage();
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            responseString = Messages.INTERNAL_ERROR;
        }
        return JsonUtils.constructJsonResponseObject(responseString);
    }

    public static Single<String> uploadFunction(int allocate,
                                         String username,
                                         String functionName,
                                         String arguments,
                                         byte[] functionCode) {
        String responseString = null;
        try {
            if (!Configuration.isInitialized()) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                responseString = Messages.NO_CONFIGURATION_UPLOADED;
            }

            String encodedName = Configuration.coder.encode(username, functionName);
            Function function = Configuration.storage.register(encodedName);
            function.setArguments(arguments);
            for (int i = 0; i < allocate; i++) {
                Configuration.functionWriter.upload(function, encodedName, functionCode);
            }

            Logger.log(Level.INFO, String.format(Messages.SUCCESS_FUNCTION_UPLOAD, functionName));
            responseString = String.format(Messages.SUCCESS_FUNCTION_UPLOAD, functionName);
        } catch (IOException | ErrorUploadingLambda e) {
            Logger.log(Level.SEVERE, String.format(Messages.ERROR_FUNCTION_UPLOAD, functionName), e);
            responseString = String.format(Messages.ERROR_FUNCTION_UPLOAD, functionName);
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            responseString = Messages.INTERNAL_ERROR;
        }
        return JsonUtils.constructJsonResponseObject(responseString);
    }

    public static Single<String> removeFunction(String username, String functionName) {
        String responseString = null;
        try {
            if (!Configuration.isInitialized()) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                responseString = Messages.NO_CONFIGURATION_UPLOADED;
            }

            String encodedName = Configuration.coder.encode(username, functionName);
            Configuration.storage.unregister(encodedName);
            Configuration.functionWriter.remove(encodedName);

            Logger.log(Level.INFO, String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName));
            responseString = String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName);
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            responseString = Messages.INTERNAL_ERROR;
        }
        return JsonUtils.constructJsonResponseObject(responseString);
    }

    public static Single<String> configureManager(String lambdaManagerConfiguration, BeanContext beanContext) {
        String responseString = null;
        try {
            ArgumentStorage.initializeLambdaManager(ArgumentParser.parse(lambdaManagerConfiguration), beanContext);
            Logger.log(Level.INFO, Messages.SUCCESS_CONFIGURATION_UPLOAD);
            responseString = Messages.SUCCESS_CONFIGURATION_UPLOAD;
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
        Object response = null;
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
