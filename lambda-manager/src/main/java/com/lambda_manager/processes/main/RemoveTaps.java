package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.ConnectionTriplet;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.client.RxHttpClient;

import java.util.List;

import static com.lambda_manager.utils.Constants.REMOVE_TAPS_FILENAME;

public class RemoveTaps extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        command.add("bash");
        command.add("src/scripts/remove_taps.sh");
        for (ConnectionTriplet<String, String, RxHttpClient> connectionTriplet :
                configuration.argumentStorage.getConnectionPool()) {
            command.add(connectionTriplet.tap);
        }
        return command;
    }

    @Override
    protected String outputFilename(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return REMOVE_TAPS_FILENAME;
    }
}
