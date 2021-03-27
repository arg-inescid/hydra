package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

public class RemoveTaps extends AbstractProcess {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_taps.sh");
//        for(String lambdaName : configuration.storage.getAll().keySet()) {
//            int numberOfInstances = configuration.argumentStorage.getNumberOfInstances(lambdaName);
//            for (int i = 0; i < numberOfInstances; i++) {
//                command.add(configuration.argumentStorage.getTapName(lambdaName, i));
//            }
//        }
        for(Tuple<String, String> tapIp: configuration.argumentStorage.getTapIPPool()) {
            command.add(tapIp.list);
        }
        return command;
    }
}
