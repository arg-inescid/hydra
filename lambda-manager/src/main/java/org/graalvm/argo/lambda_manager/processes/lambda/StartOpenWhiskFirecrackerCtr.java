package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

import java.util.List;

public class StartOpenWhiskFirecrackerCtr extends StartFirecrackerCtr {

    public StartOpenWhiskFirecrackerCtr(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        lambda.setExecutionMode(LambdaExecutionMode.CUSTOM);
        return prepareCommand(function.getRuntime());
    }
}
