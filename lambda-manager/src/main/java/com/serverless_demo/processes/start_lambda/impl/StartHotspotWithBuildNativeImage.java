package com.serverless_demo.processes.start_lambda.impl;

import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.callbacks.impl.CheckClientStatus;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.processes.Processes;
import com.serverless_demo.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

public class StartHotspotWithBuildNativeImage extends StartHotspot {
    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        command = new ArrayList<>();
        Processes.BUILD_NATIVE_IMAGE.build(lambda, configuration).start();
        return super.makeCommand(lambda, configuration);
    }

    @Override
    public boolean destroyForcibly() {
        return false;
    }

    @Override
    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new CheckClientStatus(lambda, configuration);
    }
}
