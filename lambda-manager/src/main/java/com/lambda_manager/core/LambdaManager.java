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

public class LambdaManager {

    private static final LambdaManager LAMBDA_MANAGER = new LambdaManager();
    private LambdaManagerConfiguration configuration;

    public static LambdaManager getLambdaManager() {
        return LAMBDA_MANAGER;
    }

    public LambdaManagerConfiguration getConfiguration() {
        return configuration;
    }

    public Single<String> processRequest(String username, String lambdaName, String args) {
        try {
            if (configuration == null) {
                return Single.just("No configuration has been uploaded!");
            }
            long start = System.currentTimeMillis();
            String encodedName = configuration.encoder.encode(username, lambdaName);
            Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda = configuration.scheduler.schedule(encodedName, args, configuration);
            String response = configuration.client.sendRequest(lambda, configuration);
            configuration.optimizer.registerCall(lambda, configuration);
            configuration.scheduler.reschedule(lambda, configuration);
            System.out.println("Request spent time " + lambda.instance.getId() + ": " + (System.currentTimeMillis() - start) + "[ms]");
            return Single.just(response);
        } catch (LambdaNotFound lambdaNotFound) {
            return Single.just(lambdaNotFound.getMessage());
        }
    }

    public Single<String> uploadLambda(String username, String lambdaName, byte[] lambdaCode) {
        try {
            if (configuration == null) {
                return Single.just("No configuration has been uploaded!");
            }
            String encodedName = configuration.encoder.encode(username, lambdaName);
            LambdaInstancesInfo lambdaInstancesInfo = configuration.storage.register(encodedName);
            Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda = configuration.codeWriter.upload(lambdaInstancesInfo, encodedName, lambdaCode);
//            Processes.CREATE_TAP.build(lambda, configuration).start();
//            configuration.client.createNewClient(lambda, configuration, true);
            return Single.just("Successfully uploaded lambda [ " + lambdaName + " ]!");
        } catch (IOException | ErrorUploadingNewLambda e) {
            e.printStackTrace(System.err);
            return Single.just("Error during uploading new lambda [ " + lambdaName + " ]!");
        }
    }

    public Single<String> removeLambda(String username, String lambdaName) {
        if (configuration == null) {
            return Single.just("No configuration has been uploaded!");
        }
        String encodedName = configuration.encoder.encode(username, lambdaName);
        configuration.storage.unregister(encodedName);
        configuration.codeWriter.remove(encodedName);
        return Single.just("Successfully removed lambda [ " + lambdaName + " ]!");
    }

    public Single<String> startManager(String configData) {
        try {
            configuration = new LambdaManagerArgumentStorage().initializeLambdaManager(ArgumentParser.parse(configData));
            return Single.just("Successfully uploaded lambda manager configuration!");
        } catch (InvalidJSONFile invalidJSONFile) {
            invalidJSONFile.printStackTrace(System.err);
            return Single.just("Invalid JSON syntax!");
        } catch (ErrorDuringParsingJSONFile errorDuringParsingJSONFile) {
            errorDuringParsingJSONFile.printStackTrace(System.err);
            return Single.just("Error during parsing JSON config file!");
        } catch (ErrorDuringReflectiveClassCreation errorDuringReflectiveClassCreation) {
            errorDuringReflectiveClassCreation.printStackTrace(System.err);
            return Single.just(errorDuringReflectiveClassCreation.getMessage());
        }
    }
}
