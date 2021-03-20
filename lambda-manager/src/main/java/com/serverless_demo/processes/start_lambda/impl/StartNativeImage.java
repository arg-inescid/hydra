package com.serverless_demo.processes.start_lambda.impl;

import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.callbacks.impl.DefaultCallback;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.processes.start_lambda.StartLambda;
import com.serverless_demo.utils.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StartNativeImage extends StartLambda {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        command = new ArrayList<>();
        String lambdaName = lambda.list.getName();
        int lambdaId = lambda.instance.getId();

        command.add("bash");
        command.add("src/lambdas/" + lambdaName + "/" + lambdaName + "_unikernel.sh");
        command.add("--memory");
        command.add(configuration.argumentStorage.getMemorySpace());
        command.add("--ip");
        command.add(configuration.argumentStorage.getInstanceAddress(lambdaName, lambdaId));
        command.add("--tap");
        command.add(configuration.argumentStorage.getTapName(lambdaName, lambdaId));
        if(configuration.argumentStorage.isConsoleActive()) {
            command.add("--console");
        }
        if(lambda.instance.getArgs() != null) {
            Collections.addAll(command, lambda.instance.getArgs().split(","));
        }
        System.out.println("NativeImage " + Arrays.toString(command.toArray()));
        return command;
    }

    @Override
    public boolean destroyForcibly() {
        return true;
    }

    @Override
    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new DefaultCallback();
    }
}
