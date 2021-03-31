package com.lambda_manager.processes.main;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.NativeImageBuiltCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.Tuple;

import java.util.List;

public class BuildNativeImage extends AbstractProcess {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();

        String lambdaName = lambda.list.getName();
        command.add("bash");
        command.add("src/scripts/build_vmm.sh");
        command.add(configuration.argumentStorage.getExecBinaries() + "/bin");
        command.add("src/lambdas/" + lambdaName);
        command.add(lambdaName + ".jar");
        command.add(configuration.argumentStorage.getVirtualizationConfig());
        return command;
    }

    @Override
    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new NativeImageBuiltCallback(lambda, configuration);
    }

    @Override
    public String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return processOutputFile == null ? "src/lambdas/" + lambda.list.getName() + "/outputs/build-native-image_" +
                configuration.argumentStorage.generateRandomString() + ".dat" : processOutputFile;
    }
}
