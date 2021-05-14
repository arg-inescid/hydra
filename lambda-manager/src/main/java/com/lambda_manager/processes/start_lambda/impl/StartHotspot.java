package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.CopyOutputLogFileCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.ConnectionTriplet;
import com.lambda_manager.core.Environment;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.client.RxHttpClient;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.lambda_manager.core.Environment.*;

public class StartHotspot extends StartLambda {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        lambda.instance.setExecutionMode(LambdaExecutionMode.HOTSPOT);
        this.outputFilename = outputFilename(lambda, configuration);
        this.memoryFilename = memoryFilename(lambda, configuration);
        ConnectionTriplet<String, String, RxHttpClient> connectionTriplet = lambda.lambda.getConnectionTriplet();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.memoryFilename);
        command.add("-v");
        command.add("bash");
        // TODO: Replace hardcoded paths with Paths.get().
        command.add("src/scripts/start_hotspot.sh");
        command.add(lambda.function.getName());
        command.add(String.valueOf(lambda.lambda.getId()));
        command.add(configuration.argumentStorage.getMemorySpace());
        command.add(connectionTriplet.ip);
        command.add(connectionTriplet.tap);
        command.add(configuration.argumentStorage.getGateway());
        command.add(configuration.argumentStorage.getMask());
        if (configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("");    // Placeholder.
        }
        command.add(String.valueOf(configuration.argumentStorage.getLambdaPort()));
        if (lambda.lambda.getArgs() != null) {
            Collections.addAll(command, lambda.lambda.getArgs().split(","));
        }
        command.add(String.valueOf(System.currentTimeMillis()));
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        String sourceFile = Paths.get(CODEBASE,
                lambda.function.getName(),
                "start-hotspot-id-" + lambda.lambda.getId(),
                RUN_LOG)
                .toString();
        return new CopyOutputLogFileCallback(sourceFile, this.outputFilename);
    }

    @Override
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        String dirPath = Paths.get(LAMBDA_LOGS, lambda.function.getName(), String.valueOf(lambda.lambda.getId()), HOTSPOT)
                .toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdirs();
        return outputFilename == null ?
                Paths.get(dirPath, "output_" + pid() + ".log").toString()
                : outputFilename;
    }

    @Override
    protected String memoryFilename(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        String dirPath = Paths.get(LAMBDA_LOGS, lambda.function.getName(), String.valueOf(lambda.lambda.getId()), HOTSPOT)
                .toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdirs();
        return memoryFilename == null ?
                Paths.get(dirPath, "memory_" + pid() + ".log").toString()
                : memoryFilename;
    }

    @Override
    protected long pid() {
        if (this.pid == -1) {
            this.pid = Environment.pid();
        }
        return this.pid;
    }
}
