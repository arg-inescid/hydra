package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public class StartOpenWhiskContainer extends StartContainer {

    public StartOpenWhiskContainer(Lambda lambda) {
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
        command.add("src/scripts/start_openwhisk_container.sh");
        command.add(String.valueOf(connection.port));
        command.add(lambda.getLambdaName());
        command.add(lambda.getExecutionMode().getOpenWhiskContainerImage());
        // Convert memory to bytes for the "docker run --memory ..." option.
        command.add(String.valueOf(Configuration.argumentStorage.getMaxMemory() * 1024l * 1024l));
        command.add(String.valueOf(Configuration.argumentStorage.getCpuQuota()));
        return command;
    }
}
