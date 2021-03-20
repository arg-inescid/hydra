package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.CheckClientStatus;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.utils.Tuple;

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
