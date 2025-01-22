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
        List<String> command = prepareCommand(lambda.getExecutionMode().getOpenWhiskContainerImage());
        // Convert memory to bytes for the "docker run --memory ..." option.
        command.add(String.valueOf(Configuration.argumentStorage.getMaxMemory() * 1024l * 1024l));
        command.add(String.valueOf(Configuration.argumentStorage.getCpuQuota()));
        return command;
    }
}
