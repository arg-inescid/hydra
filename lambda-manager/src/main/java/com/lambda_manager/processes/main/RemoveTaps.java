package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.Tuple;

import java.util.List;

public class RemoveTaps extends AbstractProcess {

    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        command.add("bash");
        command.add("src/scripts/remove_taps.sh");
        for (LambdaInstanceInfo instance : lambda.list.getAvailableInstances()) {
            command.add(instance.getTap());
        }
        return command;
    }

    @Override
    public String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/outputs/remove-taps_"
                + configuration.argumentStorage.generateRandomString() + ".dat" : processOutputFile;
    }
}
