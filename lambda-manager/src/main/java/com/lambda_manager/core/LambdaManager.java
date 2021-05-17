package com.lambda_manager.core;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import com.lambda_manager.exceptions.user.ErrorDuringCreatingConnectionPool;
import com.lambda_manager.exceptions.user.ErrorUploadingLambda;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.utils.LambdaTuple;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;
import com.lambda_manager.utils.parser.ArgumentParser;
import io.micronaut.context.BeanContext;
import io.reactivex.Single;

import java.io.IOException;
import java.util.logging.Level;

public class LambdaManager {

    private static LambdaManager lambdaManager;
    private static LambdaManagerConfiguration configuration;

    private LambdaManager() {
    }

    public static LambdaManager getLambdaManager() {
        if (lambdaManager == null) {
            lambdaManager = new LambdaManager();
        }
        return lambdaManager;
    }

    public static LambdaManagerConfiguration getConfiguration() {
        return configuration;
    }

    private String formatRequestSpentTimeMessage(LambdaTuple<Function, Lambda> lambda, long spentTime) {
        switch (lambda.function.getStatus()) {
            case NOT_BUILT_NOT_CONFIGURED:
                return String.format(Messages.TIME_HOTSPOT_W_AGENT, lambda.lambda.getId(), spentTime);
            case CONFIGURING_OR_BUILDING:
            case NOT_BUILT_CONFIGURED:
                return String.format(Messages.TIME_HOTSPOT, lambda.lambda.getId(), spentTime);
            case BUILT:
                return String.format(Messages.TIME_NATIVE_IMAGE, lambda.lambda.getId(), spentTime);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Single<String> processRequest(String username, String functionName, String functionArguments) {
        try {
            if (configuration == null) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                return Single.just(Messages.NO_CONFIGURATION_UPLOADED);
            }

            long start = System.currentTimeMillis();
            String encodedName = configuration.encoder.encode(username, functionName);
            LambdaTuple<Function, Lambda> lambda = configuration.scheduler.schedule(encodedName,
                    functionArguments, configuration);

            String response = configuration.client.sendRequest(lambda, configuration);
            configuration.optimizer.registerCall(lambda, configuration);
            configuration.scheduler.reschedule(lambda, configuration);

            Logger.log(Level.FINE, formatRequestSpentTimeMessage(lambda, System.currentTimeMillis() - start));
            return Single.just(response);
        } catch (FunctionNotFound functionNotFound) {
            Logger.log(Level.WARNING, functionNotFound.getMessage(), functionNotFound);
            return Single.just(functionNotFound.getMessage());
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            return Single.just(Messages.INTERNAL_ERROR);
        }
    }

    public Single<String> uploadFunction(int allocate, String username, String functionName, byte[] functionCode) {
        try {
            if (configuration == null) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                return Single.just(Messages.NO_CONFIGURATION_UPLOADED);
            }

            String encodedName = configuration.encoder.encode(username, functionName);
            Function function = configuration.storage.register(encodedName);
            for (int i = 0; i < allocate; i++) {
                configuration.functionWriter.upload(function, encodedName, functionCode);
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

    public Single<String> removeFunction(String username, String functionName) {
        try {
            if (configuration == null) {
                Logger.log(Level.WARNING, Messages.NO_CONFIGURATION_UPLOADED);
                return Single.just(Messages.NO_CONFIGURATION_UPLOADED);
            }

            String encodedName = configuration.encoder.encode(username, functionName);
            configuration.storage.unregister(encodedName);
            configuration.functionWriter.remove(encodedName);

            Logger.log(Level.INFO, String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName));
            return Single.just(String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName));
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            return Single.just(Messages.INTERNAL_ERROR);
        }
    }

    public Single<String> configureManager(String lambdaManagerConfiguration, BeanContext beanContext) {
        try {
            configuration = new LambdaManagerArgumentStorage().initializeLambdaManager(
                    ArgumentParser.parse(lambdaManagerConfiguration), beanContext);
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
