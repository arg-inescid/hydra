package com.lambda_manager.optimizers.impl;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.optimizers.Optimizer;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.processes.lambda.StartLambda;
import com.lambda_manager.utils.LambdaTuple;

@SuppressWarnings("unused")
public class DefaultOptimizer implements Optimizer {

    @Override
    public void registerCall(LambdaTuple<Function, Lambda> lambda) {
    	// TODO - while we don't have use for it, delete it.
    }

    @Override
    public StartLambda whomToSpawn(LambdaTuple<Function, Lambda> lambda) {
        AbstractProcess process;
        switch (lambda.function.getStatus()) {
            case NOT_BUILT_NOT_CONFIGURED:
                process = Processes.START_HOTSPOT_WITH_AGENT;
                lambda.function.setLastAgentPID(lambda.lambda.pid());
                lambda.function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
                break;
            case NOT_BUILT_CONFIGURED:
                Processes.BUILD_VMM.build(lambda).start();
                lambda.function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
            case CONFIGURING_OR_BUILDING:
                process = Processes.START_HOTSPOT;
                break;
            case BUILT:
                process = Processes.START_NATIVE_IMAGE;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + lambda.function.getStatus());
        }

        return (StartLambda) process;
    }
}
