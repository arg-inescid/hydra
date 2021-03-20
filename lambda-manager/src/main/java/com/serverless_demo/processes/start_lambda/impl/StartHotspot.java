package com.serverless_demo.processes.start_lambda.impl;

import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.callbacks.impl.CheckClientStatus;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.processes.start_lambda.StartLambda;
import com.serverless_demo.utils.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartHotspot extends StartLambda {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        command = new ArrayList<>();
        String lambdaName = lambda.list.getName();
        command.add("java");
        command.add("-Dmicronaut.server.port=" + configuration.argumentStorage.getInstancePort(lambdaName, lambda.instance.getId()));
        command.add("-jar");
        command.add("src/lambdas/" + lambdaName + "/" + lambdaName + ".jar");
        if(lambda.instance.getArgs() != null) {
            Collections.addAll(command, lambda.instance.getArgs().split(","));
        }
        return command;
    }

    @Override
    public boolean destroyForcibly() {
        return false;
    }

    @Override
    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new CheckClientStatus(lambda, configuration);
    }
}
