package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.DefaultCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.connectivity.client.impl.DefaultLambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.Tuple;

import java.util.Collections;
import java.util.List;

public class StartNativeImage extends StartLambda {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();

        int port = configuration.argumentStorage.getLambdaListeningPort();
        lambda.instance.setPort(-1);
        lambda.instance.setHttpClient(DefaultLambdaManagerClient.newClient(lambda.instance.getIp(), port, false));
        String lambdaName = lambda.list.getName();
        int lambdaId = lambda.instance.getId();
        this.processOutputFile = processOutputFile(lambda, configuration);

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.processOutputFile);
        command.add("-v");
        command.add("bash");
        command.add("src/lambdas/" + lambdaName + "/" + lambdaName + "_unikernel.sh");
        command.add("--memory");
        command.add(configuration.argumentStorage.getMemorySpace());
        command.add("--ip");
        command.add(lambda.instance.getIp());
        command.add("--tap");
        command.add(lambda.instance.getTap());
        command.add("--gateway");
        command.add(configuration.argumentStorage.getGateway());
        command.add("--mask");
        command.add(configuration.argumentStorage.getMask());
        if(configuration.argumentStorage.isVmmConsoleActive()) {
            command.add("--console");
        }
        command.add(String.valueOf(port));
        if(lambda.instance.getArgs() != null) {
            Collections.addAll(command, lambda.instance.getArgs().split(","));
        }
        return command;
    }

    @Override
    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new DefaultCallback();
    }

    @Override
    public String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/lambdas/" + lambda.list.getName() + "/outputs/start-native-image_" +
                configuration.argumentStorage.generateRandomString() + ".dat" : processOutputFile;
    }
}
