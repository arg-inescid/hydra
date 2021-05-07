package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.AgentConfigReadyCallback;
import com.lambda_manager.callbacks.impl.CopyOutputLogFileCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.ConnectionTriplet;
import com.lambda_manager.utils.Environment;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.client.RxHttpClient;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.lambda_manager.utils.Environment.*;

public class StartHotspotWithAgent extends StartLambda {

    @Override
    protected List<String> makeCommand(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        this.outputFilename = outputFilename(lambda, configuration);
        this.memoryFilename = memoryFilename(lambda, configuration);
        ConnectionTriplet<String, String, RxHttpClient> connectionTriplet = lambda.instance.getConnectionTriplet();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.memoryFilename);
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_hotspot_agent.sh");
        command.add(lambda.list.getName());
        command.add(String.valueOf(lambda.instance.getId()));
        command.add(configuration.argumentStorage.getMemorySpace());
        command.add(connectionTriplet.ip);
        command.add(connectionTriplet.tap);
        command.add(configuration.argumentStorage.getGateway());
        command.add(configuration.argumentStorage.getMask());
        if(configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("");    // Placeholder.
        }
        command.add(String.valueOf(configuration.argumentStorage.getLambdaPort()));
        if(lambda.instance.getArgs() != null) {
            Collections.addAll(command, lambda.instance.getArgs().split(","));
        }
        command.add(String.valueOf(System.currentTimeMillis()));
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        String sourceFile = Paths.get(CODEBASE,
                lambda.list.getName(),
                "start-hotspot-w-agent-id-" + lambda.instance.getId(),
                RUN_LOG)
                .toString();
        // Nested OnProcessFinish callbacks.
        return new AgentConfigReadyCallback(lambda, new CopyOutputLogFileCallback(sourceFile, this.outputFilename));
    }

    @Override
    protected String outputFilename(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        String dirPath = Paths.get(LAMBDA_LOGS, String.valueOf(lambda.instance.getId()), lambda.list.getName(),
                HOTSPOT_W_AGENT).toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdirs();
        return outputFilename == null ?
                Paths.get(dirPath, "output_" + pid() + ".log").toString()
                : outputFilename;
    }

    @Override
    protected String memoryFilename(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        String dirPath = Paths.get(LAMBDA_LOGS, String.valueOf(lambda.instance.getId()), lambda.list.getName(),
                HOTSPOT_W_AGENT).toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdirs();
        return memoryFilename == null ?
                Paths.get(dirPath, "memory_" + pid() + ".log").toString()
                : memoryFilename;
    }

    @Override
    protected long pid() {
        if(this.pid == -1) {
            this.pid = Environment.pid();
        }
        return this.pid;
    }
}
