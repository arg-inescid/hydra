package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.callbacks.HotspotCallback;
import org.graalvm.argo.lambda_manager.callbacks.HotspotWithAgentCallback;
import org.graalvm.argo.lambda_manager.callbacks.OnProcessFinishCallback;
import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.graalvm.argo.lambda_manager.core.Environment.*;

public class StartHotspotWithAgent extends StartLambda {

    public StartHotspotWithAgent(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();

        lambda.setExecutionMode(LambdaExecutionMode.HOTSPOT_W_AGENT);
        ConnectionTriplet<String, String, RxHttpClient> connectionTriplet = lambda.getConnectionTriplet();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_hotspot_agent.sh");
        command.add(lambda.getFunction().getName());
        command.add(String.valueOf(pid));
        command.add(String.valueOf(lambda.getFunction().getMemory()));
        command.add(connectionTriplet.ip);
        command.add(connectionTriplet.tap);
        command.add(Configuration.argumentStorage.getGateway());
        command.add(Configuration.argumentStorage.getMask());
        if (Configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("--noconsole");
        }
        command.add(String.valueOf(lambda.getFunction().getLastAgentPID()));
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(ENTRY_POINT_TAG + lambda.getFunction().getEntryPoint());
        command.add(PORT_TAG + Configuration.argumentStorage.getLambdaPort());
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        String sourceFile = Paths.get(
                        CODEBASE,
                        lambda.getFunction().getName(),
                        String.format(getLambdaDirectory(), pid),
                        RUN_LOG)
                        .toString();
        // Nested OnProcessFinish callbacks.
        return new HotspotWithAgentCallback(lambda, new HotspotCallback(sourceFile, outputFilename()));
    }

    @Override
    public String getLambdaDirectory() {
        return Environment.HOTSPOT_W_AGENT;
    }
}
