package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

import java.util.List;

public class StartHotspotFirecrackerCtr extends StartFirecrackerCtr {

    public StartHotspotFirecrackerCtr(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        lambda.setExecutionMode(LambdaExecutionMode.HOTSPOT);
        return prepareCommand(Environment.HOTSPOT_DOCKER_RUNTIME);
    }
}
