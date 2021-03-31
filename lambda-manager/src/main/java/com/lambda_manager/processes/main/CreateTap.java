package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.Tuple;

import java.util.List;

public class CreateTap extends AbstractProcess {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();

        Tuple<String, String> tapIp = configuration.argumentStorage.getTapIp();
        command.add("bash");
        command.add("src/scripts/create_taps.sh");
        command.add(tapIp.list);
        command.add(tapIp.instance);
        configuration.argumentStorage.returnTapIp(tapIp);
        return command;
    }
}
