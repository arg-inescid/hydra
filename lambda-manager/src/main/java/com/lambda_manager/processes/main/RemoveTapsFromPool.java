package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.ConnectionTriplet;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.client.RxHttpClient;

import java.util.ArrayList;
import java.util.List;

import static com.lambda_manager.core.Environment.REMOVE_TAPS_FILENAME;

public class RemoveTapsFromPool extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda) {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_taps.sh");
        for (ConnectionTriplet<String, String, RxHttpClient> connectionTriplet :
                Configuration.argumentStorage.getConnectionPool()) {
            command.add(connectionTriplet.tap);
        }
        return command;
    }

    @Override
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda) {
        return REMOVE_TAPS_FILENAME;
    }
}
