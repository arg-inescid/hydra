package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartHotspotWithAgentContainer extends StartContainer {

    public StartHotspotWithAgentContainer(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = prepareCommand("argo-hotspot-agent:latest");
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

            @Override
            public void finish(int exitCode) {
                lambda.updateFunctionStatus();
                lambda.resetRegisteredInLambda();
            }
        };
    }

}
