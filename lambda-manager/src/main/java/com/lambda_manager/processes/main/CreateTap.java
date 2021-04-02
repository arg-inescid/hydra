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

        String tap = configuration.argumentStorage.generateRandomString();
        String ip = configuration.argumentStorage.getNextIPAddress();
        lambda.instance.setTap(tap);
        lambda.instance.setIp(ip);

        command.add("bash");
        command.add("src/scripts/create_taps.sh");
        command.add(tap);
        command.add(ip);
        return command;
    }

    @Override
    public String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/lambdas/" + lambda.list.getName() + "/logs/create-taps_" +
                configuration.argumentStorage.generateRandomString() + ".dat" : processOutputFile;
    }
}
