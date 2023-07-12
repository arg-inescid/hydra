package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

public class RestoreGraalvisorFirecracker extends RestoreFirecracker {

    public RestoreGraalvisorFirecracker(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        lambda.setExecutionMode(LambdaExecutionMode.GRAALVISOR);

        return prepareCommand("graalvisor");
    }

}
