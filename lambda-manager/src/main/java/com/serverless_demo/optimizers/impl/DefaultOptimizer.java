package com.serverless_demo.optimizers.impl;

import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.optimizers.LambdaStatusType;
import com.serverless_demo.optimizers.Optimizer;
import com.serverless_demo.processes.AbstractProcess;
import com.serverless_demo.processes.Processes;
import com.serverless_demo.processes.start_lambda.StartLambda;
import com.serverless_demo.utils.Tuple;

public class DefaultOptimizer implements Optimizer {
    @Override
    public void registerCall(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
    }

    @Override
    public StartLambda whomToSpawn(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        AbstractProcess process;
        switch (lambda.list.getStatus()) {
            case NOT_BUILT_NOT_CONFIGURED:
                process = Processes.START_HOTSPOT_WITH_AGENT;
                lambda.list.setStatus(LambdaStatusType.CONFIGURING_OR_BUILDING);
                break;
            case NOT_BUILT_CONFIGURED:
                process = Processes.START_HOTSPOT_WITH_BUILD;
                lambda.list.setStatus(LambdaStatusType.CONFIGURING_OR_BUILDING);
                break;
            case CONFIGURING_OR_BUILDING:
                process = Processes.START_HOTSPOT;
                break;
            case BUILT:
                process = Processes.START_NATIVE_IMAGE;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + lambda.list.getStatus());
        }

        return (StartLambda) process;
    }
}
