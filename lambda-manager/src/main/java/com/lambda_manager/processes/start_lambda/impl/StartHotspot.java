package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.DefaultCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.ConnectionTriplet;
import com.lambda_manager.utils.LambdaTuple;
import io.micronaut.http.client.RxHttpClient;

import java.util.Collections;
import java.util.List;

public class StartHotspot extends StartLambda {

    @Override
    protected List<String> makeCommand(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        this.processOutputFile = processOutputFile(lambda, configuration);
        ConnectionTriplet<String, String, RxHttpClient> connectionTriplet = lambda.instance.getConnectionTriplet();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.processOutputFile);
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_hotspot.sh");
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
        return new DefaultCallback();
    }

    @Override
    protected String processOutputFile(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/lambdas/" + lambda.list.getName() + "/logs/start-hotspot-id-" +
                lambda.instance.getId() + "_" + configuration.argumentStorage.generateRandomString() + ".dat"
                : processOutputFile;
    }
}
