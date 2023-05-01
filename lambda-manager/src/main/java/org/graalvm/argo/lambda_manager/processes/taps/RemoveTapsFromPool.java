package org.graalvm.argo.lambda_manager.processes.taps;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.processes.AbstractProcess;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.ArrayList;
import java.util.List;

public class RemoveTapsFromPool extends AbstractProcess {

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_taps.sh");
        for (LambdaConnection connection : Configuration.argumentStorage.getConnectionPool()) {
            command.add(connection.tap);
        }
        return command;
    }

    @Override
    protected String outputFilename() {
        return Environment.REMOVE_TAPS_FILENAME;
    }
}
