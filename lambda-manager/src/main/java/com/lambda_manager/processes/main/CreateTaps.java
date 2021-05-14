package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.ConnectionTriplet;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.client.RxHttpClient;

import java.util.List;
import static com.lambda_manager.core.Environment.CREATE_TAPS_FILENAME;

public class CreateTaps extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
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
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        return CREATE_TAPS_FILENAME;
    }
}
