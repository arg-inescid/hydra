package com.serverless_demo.processes.main;

import com.serverless_demo.callbacks.impl.NativeImageBuiltCallback;
import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.processes.AbstractProcess;
import com.serverless_demo.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

public class BuildNativeImage extends AbstractProcess {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        command = new ArrayList<>();
        String lambdaName = lambda.list.getName();
        command.add("bash");
        command.add("src/scripts/build_lambda.sh");
        command.add("src/lambdas/" + lambdaName);
        command.add(lambdaName + ".jar");
        command.add(configuration.argumentStorage.getVirtualizationConfig());
        return command;
    }

    @Override
    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new NativeImageBuiltCallback(lambda, configuration);
    }
}
