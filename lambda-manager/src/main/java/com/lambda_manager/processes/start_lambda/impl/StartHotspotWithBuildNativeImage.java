package com.lambda_manager.processes.start_lambda.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.utils.Tuple;

import java.util.List;

public class StartHotspotWithBuildNativeImage extends StartHotspot {

    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        Processes.BUILD_NATIVE_IMAGE.build(lambda, configuration).start();
        return super.makeCommand(lambda, configuration);
    }
}
