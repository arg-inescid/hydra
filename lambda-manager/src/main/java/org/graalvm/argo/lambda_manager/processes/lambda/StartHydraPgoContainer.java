package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.List;

public class StartHydraPgoContainer extends StartContainer {

    public StartHydraPgoContainer(Lambda lambda) {
        super(lambda);
    }

    protected List<String> makeCommand() {
        List<String> command = prepareCommand("hydra:latest");
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add("LD_LIBRARY_PATH=/lib:/lib64:/tmp/apps:/usr/local/lib");
        return command;
    }
}
