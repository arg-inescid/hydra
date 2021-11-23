package org.graalvm.argo.lambda_manager.processes.taps;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.processes.AbstractProcess;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.ArrayList;
import java.util.List;

public class RemoveTapsFromPool extends AbstractProcess {

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_taps.sh");
        for (ConnectionTriplet<String, String, RxHttpClient> connectionTriplet : Configuration.argumentStorage.getConnectionPool()) {
            command.add(connectionTriplet.tap);
        }
        return command;
    }

    @Override
    protected String outputFilename() {
        return Environment.REMOVE_TAPS_FILENAME;
    }
}
