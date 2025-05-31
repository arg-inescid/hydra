package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartGraalvisorContainer extends StartContainer {

    public StartGraalvisorContainer(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = prepareCommand("graalvisor:latest");
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        return command;
    }
}
