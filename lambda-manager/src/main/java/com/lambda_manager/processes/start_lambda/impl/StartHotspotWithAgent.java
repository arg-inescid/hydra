package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.callbacks.impl.AgentConfigReadyCallback;
import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.connectivity.client.impl.DefaultLambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartHotspotWithAgent extends StartLambda {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        command = new ArrayList<>();
        int port = configuration.argumentStorage.getNextPort();
        lambda.instance.setPort(port);
        lambda.instance.setHttpClient(DefaultLambdaManagerClient.newClient(null, port, true));
        String lambdaName = lambda.list.getName();
        String execBinaries = configuration.argumentStorage.getExecBinaries();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=src/outputs/" + configuration.argumentStorage.getVmmmLogFile());
        command.add("-v");
        command.add(execBinaries + "/bin/java");
        command.add("-Djava.library.path=" + execBinaries + "/lib");
//        command.add("-Dmicronaut.server.port=" + port);
        command.add("-agentlib:native-image-agent=config-output-dir=" + "src/lambdas/" + lambdaName + "/config"
                + ",caller-filter-file=src/main/resources/caller-filter-config.json");
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
        return new AgentConfigReadyCallback(lambda);
    }

    @Override
    public String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return "src/lambdas/" + lambda.list.getName() + "/outputs/start-hotspot-agent_" +
                configuration.argumentStorage.generateRandomString() + ".dat";
    }
}
