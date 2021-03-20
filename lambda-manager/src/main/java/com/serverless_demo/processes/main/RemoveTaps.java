package com.serverless_demo.processes.main;

import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.processes.AbstractProcess;
import com.serverless_demo.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

public class RemoveTaps extends AbstractProcess {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_taps.sh");
        for(String lambdaName : configuration.storage.getAll().keySet()) {
            int numberOfInstances = configuration.argumentStorage.getNumberOfInstances(lambdaName);
            for (int i = 0; i < numberOfInstances; i++) {
                command.add(configuration.argumentStorage.getTapName(lambdaName, i));
            }
        }
        return command;
    }
}
