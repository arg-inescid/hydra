package com.lambda_manager.core;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import com.lambda_manager.exceptions.argument_parser.InvalidJSONFile;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.exceptions.user.LambdaNotFound;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.utils.LambdaManagerArgumentStorage;
import com.lambda_manager.utils.Tuple;
import com.lambda_manager.utils.parser.ArgumentParser;
import io.reactivex.Single;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LambdaManager {

    private static LambdaManager lambdaManager;
    private static LambdaManagerConfiguration configuration;
    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private LambdaManager() {
    }

    public static LambdaManager getLambdaManager() {
        if(lambdaManager == null) {
            lambdaManager = new LambdaManager();
        }
        return lambdaManager;
    }

    public static LambdaManagerConfiguration getConfiguration() {
        return configuration;
    }

    public Single<String> processRequest(String username, String lambdaName, String args) {
        try {
            if (configuration == null) {
                logger.log(Level.WARNING, "No configuration has been uploaded!");
                return Single.just("No configuration has been uploaded!");
            }

            long start = System.currentTimeMillis();
            String encodedName = configuration.encoder.encode(username, lambdaName);
            Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda = configuration.scheduler.schedule(encodedName, args, configuration);
            String response = configuration.client.sendRequest(lambda, configuration);
            configuration.optimizer.registerCall(lambda, configuration);
            configuration.scheduler.reschedule(lambda, configuration);

            logger.log(Level.INFO,
                    "Time (vmm_id=" + lambda.instance.getId() + "): " + (System.currentTimeMillis() - start) + "\t[ms]");
            return Single.just(response);
        } catch (LambdaNotFound lambdaNotFound) {
            logger.log(Level.WARNING, lambdaNotFound.getMessage(), lambdaNotFound);
            return Single.just(lambdaNotFound.getMessage());
        }
    }

    public Single<String> uploadLambda(int allocate, String username, String lambdaName, byte[] lambdaCode) {
        try {
            if (configuration == null) {
                logger.log(Level.WARNING, "No configuration has been uploaded!");
                return Single.just("No configuration has been uploaded!");
            }

            String encodedName = configuration.encoder.encode(username, lambdaName);
            LambdaInstancesInfo lambdaInstancesInfo = configuration.storage.register(encodedName);
            for(int i = 0; i < allocate; i++) {
                Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda = configuration.codeWriter.upload(
                        lambdaInstancesInfo, encodedName, lambdaCode);
                 Processes.CREATE_TAP.build(lambda, configuration).start();
                 configuration.client.createNewClient(lambda, configuration);
            }

            logger.log(Level.INFO, "Successfully uploaded lambda [" + lambdaName + "]!");
            return Single.just("Successfully uploaded lambda [" + lambdaName + "]!");
        } catch (IOException | ErrorUploadingNewLambda e) {
            logger.log(Level.SEVERE, "Error during uploading new lambda [" + lambdaName + "]!", e);
            return Single.just("Error during uploading new lambda [" + lambdaName + "]!");
        }
    }

    public Single<String> removeLambda(String username, String lambdaName) {
        if (configuration == null) {
            logger.log(Level.WARNING, "No configuration has been uploaded!");
            return Single.just("No configuration has been uploaded!");
        }

        String encodedName = configuration.encoder.encode(username, lambdaName);
        configuration.storage.unregister(encodedName);
        configuration.codeWriter.remove(encodedName);

        logger.log(Level.INFO, "Successfully removed lambda [" + lambdaName + "]!");
        return Single.just("Successfully removed lambda [" + lambdaName + "]!");
    }

    public Single<String> startManager(String configData) {
        try {
            configuration = new LambdaManagerArgumentStorage().initializeLambdaManager(ArgumentParser.parse(configData));
            logger.log(Level.INFO, "Successfully uploaded lambda manager configuration!");
            return Single.just("Successfully uploaded lambda manager configuration!");
        } catch (InvalidJSONFile invalidJSONFile) {
            logger.log(Level.SEVERE, "Invalid JSON syntax!", invalidJSONFile);
            return Single.just("Invalid JSON syntax!");
        } catch (ErrorDuringParsingJSONFile errorDuringParsingJSONFile) {
            logger.log(Level.SEVERE, "Error during parsing JSON config file!", errorDuringParsingJSONFile);
            return Single.just("Error during parsing JSON config file!");
        } catch (ErrorDuringReflectiveClassCreation errorDuringReflectiveClassCreation) {
            logger.log(Level.SEVERE, errorDuringReflectiveClassCreation.getMessage(), errorDuringReflectiveClassCreation);
            return Single.just(errorDuringReflectiveClassCreation.getMessage());
        }
    }
}
