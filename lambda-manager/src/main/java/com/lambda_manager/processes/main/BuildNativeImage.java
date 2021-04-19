package com.lambda_manager.processes.main;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.NativeImageBuiltCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.util.List;

public class BuildNativeImage extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        this.processOutputFile = processOutputFile(lambda, configuration);

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.processOutputFile);
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/build_vmm.sh");
        command.add(lambda.list.getName());
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new NativeImageBuiltCallback(lambda);
    }

    @Override
    protected String processOutputFile(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/lambdas/" + lambda.list.getName() + "/logs/build-native-image_"
                + configuration.argumentStorage.generateRandomString() + ".dat" : processOutputFile;
    }
}
