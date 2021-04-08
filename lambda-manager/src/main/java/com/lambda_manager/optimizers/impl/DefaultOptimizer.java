package com.lambda_manager.optimizers.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.optimizers.Optimizer;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.Tuple;

@SuppressWarnings("unused")
public class DefaultOptimizer implements Optimizer {

    @Override
    public void registerCall(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
    	// TODO - while we don't have use for it, delete it.
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
