package com.lambda_manager.processes.lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.HotspotCallback;
import com.lambda_manager.callbacks.impl.HotspotWithAgentCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.core.Environment;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.processes.lambda.StartLambda;
import com.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lambda_manager.core.Environment.*;

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
        command.add(Configuration.argumentStorage.getMemorySpace());
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
        command.add(String.valueOf(System.currentTimeMillis()));
        command.add(lambda.getFunction().getEntryPoint());
        if (lambda.getFunction().getArguments() != null) {
            Collections.addAll(command, lambda.getFunction().getArguments().split(","));
        }
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
