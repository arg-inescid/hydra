package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.CheckClientStatus;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.connectivity.client.impl.DefaultLambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.Tuple;

import java.util.Collections;
import java.util.List;

public class StartHotspot extends StartLambda {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();

        int port = configuration.argumentStorage.getNextPort();
        lambda.instance.setPort(port);
        lambda.instance.setHttpClient(DefaultLambdaManagerClient.newClient(null, port, true));
        String lambdaName = lambda.list.getName();
        this.processOutputFile = processOutputFile(lambda, configuration);

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.processOutputFile);
        command.add("-v");
        command.add(configuration.argumentStorage.getExecBinaries() + "/bin/java");
//        command.add("-Dmicronaut.server.port=" + port);
        command.add("-jar");
        command.add("src/lambdas/" + lambdaName + "/" + lambdaName + ".jar");
        command.add(String.valueOf(port));
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

    @Override
    public String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/lambdas/" + lambda.list.getName() + "/outputs/start-hotspot_" +
                lambda.list.getId() + "_" + configuration.argumentStorage.generateRandomString() + ".dat" : processOutputFile;
    }
}
