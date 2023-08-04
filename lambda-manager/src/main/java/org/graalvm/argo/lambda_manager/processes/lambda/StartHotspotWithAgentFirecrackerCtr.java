package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.List;

public class StartHotspotWithAgentFirecrackerCtr extends StartFirecrackerCtr {

    public StartHotspotWithAgentFirecrackerCtr(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand(Environment.HOTSPOT_AGENT_DOCKER_RUNTIME);
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
