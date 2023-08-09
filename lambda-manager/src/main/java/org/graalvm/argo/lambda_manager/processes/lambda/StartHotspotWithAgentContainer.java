package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public class StartHotspotWithAgentContainer extends StartContainer {

    public StartHotspotWithAgentContainer(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        LambdaConnection connection = lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_hotspot_agent_container.sh");
        command.add(String.valueOf(pid));
        command.add(lambda.getLambdaName());
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(PORT_TAG + connection.port);
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
