package com.serverless_demo.processes.main;

import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.processes.AbstractProcess;
import com.serverless_demo.utils.Tuple;

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
