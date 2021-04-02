package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.AgentConfigReadyCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.Tuple;

import java.util.Collections;
import java.util.List;

public class StartHotspotWithAgent extends StartLambda {

    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();

        String lambdaName = lambda.list.getName();
        String execBinaries = configuration.argumentStorage.getExecBinaries();
        this.processOutputFile = processOutputFile(lambda, configuration);

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.processOutputFile);
        command.add("-v");
        command.add(execBinaries + "/bin/java");
        command.add("-Djava.library.path=" + execBinaries + "/lib");
        command.add("-agentlib:native-image-agent=config-output-dir=" + "src/lambdas/" + lambdaName + "/config"
                + ",caller-filter-file=src/main/resources/caller-filter-config.json");
        command.add("-jar");
        command.add("src/lambdas/" + lambdaName + "/" + lambdaName + ".jar");
        command.add(String.valueOf(lambda.instance.getPort()));
        if(lambda.instance.getArgs() != null) {
            Collections.addAll(command, lambda.instance.getArgs().split(","));
        }
        return command;
    }

    @Override
    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new AgentConfigReadyCallback(lambda);
    }

    @Override
    public String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/lambdas/" + lambda.list.getName() + "/outputs/start-hotspot-agent_" +
                configuration.argumentStorage.generateRandomString() + ".dat" : processOutputFile;
    }
}
