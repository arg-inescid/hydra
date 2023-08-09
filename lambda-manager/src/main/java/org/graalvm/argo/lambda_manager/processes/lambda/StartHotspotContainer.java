package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public class StartHotspotContainer extends StartContainer {

    public StartHotspotContainer(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        LambdaConnection connection = lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_hotspot_container.sh");
        command.add(String.valueOf(pid));
        command.add(lambda.getLambdaName());
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(PORT_TAG + connection.port);
        return command;
    }
}
