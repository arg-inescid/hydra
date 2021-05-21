package com.lambda_manager.processes.lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.VMMCallback;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.processes.lambda.StartLambda;
import com.lambda_manager.utils.ConnectionTriplet;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.client.RxHttpClient;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lambda_manager.core.Environment.*;

public class StartVMM extends StartLambda {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda) {
        List<String> command = new ArrayList<>();

        lambda.lambda.setExecutionMode(LambdaExecutionMode.NATIVE_IMAGE);
        ConnectionTriplet<String, String, RxHttpClient> connectionTriplet = lambda.lambda.getConnectionTriplet();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename(lambda)));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_vmm.sh");
        command.add(lambda.function.getName());
        command.add(String.valueOf(lambda.lambda.pid()));
        command.add(Configuration.argumentStorage.getMemorySpace());
        command.add(connectionTriplet.ip);
        command.add(connectionTriplet.tap);
        command.add(Configuration.argumentStorage.getGateway());
        command.add(Configuration.argumentStorage.getMask());
        if (Configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("");    // Placeholder.
        }
        if (lambda.function.getArguments() != null) {
            Collections.addAll(command, lambda.function.getArguments().split(","));
        }
        command.add(String.valueOf(System.currentTimeMillis()));
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<Function, Lambda> lambda) {
        return new VMMCallback(lambda);
    }

    @Override
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda) {
        String dirPath = Paths.get(
                LAMBDA_LOGS,
                lambda.function.getName(),
                String.format(VMM, lambda.lambda.pid()))
                .toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdirs();
        return Paths.get(dirPath, OUTPUT).toString();
    }

    @Override
    protected String memoryFilename(LambdaTuple<Function, Lambda> lambda) {
        String dirPath = Paths.get(
                LAMBDA_LOGS,
                lambda.function.getName(),
                String.format(VMM, lambda.lambda.pid()))
                .toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdirs();
        return Paths.get(dirPath, MEMORY).toString();
    }
}
