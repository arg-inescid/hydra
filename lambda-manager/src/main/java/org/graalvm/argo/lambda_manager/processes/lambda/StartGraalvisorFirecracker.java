package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

public class StartGraalvisorFirecracker extends StartFirecracker {

    public StartGraalvisorFirecracker(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        lambda.setExecutionMode(LambdaExecutionMode.GRAALVISOR);

        List<String> command = prepareCommand("src/scripts/start_firecracker.sh");
        command.add(lambda.getLambdaName());
        command.add("graalvisor");
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(PORT_TAG + Configuration.argumentStorage.getLambdaPort());
        command.add("LD_LIBRARY_PATH=/lib:/lib64:/tmp/apps:/usr/local/lib");
        return command;
    }

}
