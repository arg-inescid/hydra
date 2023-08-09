package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartHotspotWithAgentFirecracker extends StartFirecracker {

    public StartHotspotWithAgentFirecracker(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand("hotspot-agent");
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
