package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

public class StartHotspotFirecracker extends StartFirecracker {

    public StartHotspotFirecracker(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        lambda.setExecutionMode(LambdaExecutionMode.HOTSPOT);

        List<String> command = prepareCommand("src/scripts/start_generic_firecracker.sh");
        command.add(lambda.getLambdaName());
        command.add("hotspot");
        return command;
    }

}
