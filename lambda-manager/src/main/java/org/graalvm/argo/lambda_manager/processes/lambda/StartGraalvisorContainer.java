package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public class StartGraalvisorContainer extends StartGraalvisor {

    public StartGraalvisorContainer(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();

        lambda.setExecutionMode(LambdaExecutionMode.GRAALVISOR);
        LambdaConnection connection = lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_graalvisor_container.sh");
        command.add(String.valueOf(pid));
        command.add(lambda.getLambdaName());
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(PORT_TAG + connection.port);
        command.add("LD_LIBRARY_PATH=/lib:/lib64:/tmp/apps:/usr/local/lib");
        return command;
    }

}
