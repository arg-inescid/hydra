package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

public class CreateTap extends AbstractProcess {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/create_taps.sh");
        command.add(configuration.argumentStorage.getBridgeName());
        command.add(configuration.argumentStorage.getTapName(lambda.list.getName(), lambda.instance.getId()));
        return command;
    }
}
