package com.lambda_manager.core;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import com.lambda_manager.exceptions.user.ErrorDuringCreatingConnectionPool;
import com.lambda_manager.exceptions.user.ErrorUploadingLambda;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;
import com.lambda_manager.utils.parser.ArgumentParser;
import io.micronaut.context.BeanContext;
import io.reactivex.Single;

import java.io.IOException;
import java.util.logging.Level;

public class LambdaManager {

	/** Number of times a request will be sent to a different Lambda upon timeout. */
	// TODO - This value should be configurable.
	private static final int LAMBDA_FAULT_TOLERANCE = 3;

    private LambdaManager() {
    }

    private static String formatRequestSpentTimeMessage(Lambda lambda, long spentTime) {
        switch (lambda.getExecutionMode()) {
            case HOTSPOT_W_AGENT:
                return String.format(Messages.TIME_HOTSPOT_W_AGENT, lambda.getProcess().pid(), spentTime);
            case HOTSPOT:
                return String.format(Messages.TIME_HOTSPOT, lambda.getProcess().pid(), spentTime);
            case NATIVE_IMAGE:
                return String.format(Messages.TIME_NATIVE_IMAGE, lambda.getProcess().pid(), spentTime);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static Single<String> processRequest(String username, String functionName, String parameters) {
        try {
            if (!Configuration.isInitialized()) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                return Single.just(Messages.NO_CONFIGURATION_UPLOADED);
            }

            Function function = Configuration.storage.get(Configuration.encoder.encode(username, functionName));
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

            return Single.just(response);
        } catch (FunctionNotFound functionNotFound) {
            Logger.log(Level.WARNING, functionNotFound.getMessage(), functionNotFound);
            return Single.just(functionNotFound.getMessage());
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            return Single.just(Messages.INTERNAL_ERROR);
        }
    }

    public static Single<String> uploadFunction(int allocate,
                                         String username,
                                         String functionName,
                                         String arguments,
                                         byte[] functionCode) {
        try {
            if (!Configuration.isInitialized()) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                return Single.just(Messages.NO_CONFIGURATION_UPLOADED);
            }

            String encodedName = Configuration.encoder.encode(username, functionName);
            Function function = Configuration.storage.register(encodedName);
            function.setArguments(arguments);
            for (int i = 0; i < allocate; i++) {
                Configuration.functionWriter.upload(function, encodedName, functionCode);
            }

            Logger.log(Level.INFO, String.format(Messages.SUCCESS_FUNCTION_UPLOAD, functionName));
            return Single.just(String.format(Messages.SUCCESS_FUNCTION_UPLOAD, functionName));
        } catch (IOException | ErrorUploadingLambda e) {
            Logger.log(Level.SEVERE, String.format(Messages.ERROR_FUNCTION_UPLOAD, functionName), e);
            return Single.just(String.format(Messages.ERROR_FUNCTION_UPLOAD, functionName));
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            return Single.just(Messages.INTERNAL_ERROR);
        }
    }

    public static Single<String> removeFunction(String username, String functionName) {
        try {
            if (!Configuration.isInitialized()) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                return Single.just(Messages.NO_CONFIGURATION_UPLOADED);
            }

            String encodedName = Configuration.encoder.encode(username, functionName);
            Configuration.storage.unregister(encodedName);
            Configuration.functionWriter.remove(encodedName);

            Logger.log(Level.INFO, String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName));
            return Single.just(String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName));
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            return Single.just(Messages.INTERNAL_ERROR);
        }
    }

    public static Single<String> configureManager(String lambdaManagerConfiguration, BeanContext beanContext) {
        try {
            ArgumentStorage.initializeLambdaManager(ArgumentParser.parse(lambdaManagerConfiguration), beanContext);
            Logger.log(Level.INFO, Messages.SUCCESS_CONFIGURATION_UPLOAD);
            return Single.just(Messages.SUCCESS_CONFIGURATION_UPLOAD);
        } catch (ErrorDuringParsingJSONFile | ErrorDuringReflectiveClassCreation | ErrorDuringCreatingConnectionPool e) {
            Logger.log(Level.SEVERE, e.getMessage(), e);
            return Single.just(Messages.ERROR_CONFIGURATION_UPLOAD);
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            return Single.just(Messages.INTERNAL_ERROR);
        }
    }
}
