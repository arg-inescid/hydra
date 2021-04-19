package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.ConnectionTriplet;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.client.RxHttpClient;

import java.util.List;

public class CreateTaps extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        command.add("bash");
        command.add("src/scripts/create_taps.sh");
        for (ConnectionTriplet<String, String, RxHttpClient> connectionTriplet :
                configuration.argumentStorage.getConnectionPool()) {
            command.add(connectionTriplet.tap);
            command.add(connectionTriplet.ip);
        }
        return command;
    }

    @Override
    protected String processOutputFile(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/logs/taps/create-taps_" +
                configuration.argumentStorage.generateRandomString() + ".dat" : processOutputFile;
    }
}
