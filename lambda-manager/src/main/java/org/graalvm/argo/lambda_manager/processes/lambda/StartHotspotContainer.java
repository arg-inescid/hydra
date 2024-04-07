package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartHotspotContainer extends StartContainer {

    public StartHotspotContainer(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = prepareCommand("argo-hotspot:latest");
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        return command;
    }
}
