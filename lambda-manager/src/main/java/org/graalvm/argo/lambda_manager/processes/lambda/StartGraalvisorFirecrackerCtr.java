package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

import java.util.List;

public class StartGraalvisorFirecrackerCtr extends StartFirecrackerCtr {

    public StartGraalvisorFirecrackerCtr(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        lambda.setExecutionMode(LambdaExecutionMode.GRAALVISOR);
        return prepareCommand(Environment.GRAALVISOR_DOCKER_RUNTIME);
    }
}
