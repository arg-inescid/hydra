package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

import java.util.ArrayList;
import java.util.List;

public class StartHotspotWithAgentVM extends StartHotspotWithAgent {

    public StartHotspotWithAgentVM(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
        lambda.setExecutionMode(LambdaExecutionMode.HOTSPOT_W_AGENT);
        LambdaConnection connection = lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_hotspot_agent_vm.sh");
        command.add(function.getName());
        command.add(String.valueOf(pid));
        command.add(String.valueOf(function.getMemory()));
        command.add(connection.ip);
        command.add(connection.tap);
        command.add(Configuration.argumentStorage.getGateway());
        command.add(Configuration.argumentStorage.getMask());
        if (Configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("--noconsole");
        }
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(ENTRY_POINT_TAG + function.getEntryPoint());
        command.add(PORT_TAG + Configuration.argumentStorage.getLambdaPort());
        command.add(String.valueOf(function.getLastAgentPID()));
        return command;
    }
}
