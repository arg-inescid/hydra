package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartHydraContainer extends StartContainer {

    public StartHydraContainer(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = prepareCommand("hydra:latest");
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        return command;
    }
}
