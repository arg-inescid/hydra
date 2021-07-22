package com.lambda_manager.optimizers.impl;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.optimizers.Optimizer;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.processes.lambda.BuildVMM;
import com.lambda_manager.processes.lambda.StartLambda;
import com.lambda_manager.processes.lambda.impl.StartHotspot;
import com.lambda_manager.processes.lambda.impl.StartHotspotWithAgent;
import com.lambda_manager.processes.lambda.impl.StartVMM;

public class DefaultOptimizer implements Optimizer {

    @Override
    public void registerCall(Lambda lambda) {
    	// TODO - while we don't have use for it, delete it.
    }

    @Override
    public StartLambda whomToSpawn(Lambda lambda) {
        Function function = lambda.getFunction();
        StartLambda process;
        switch (function.getStatus()) {
            case NOT_BUILT_NOT_CONFIGURED:
                process = new StartHotspotWithAgent(lambda);
                function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
                break;
            case NOT_BUILT_CONFIGURED:
                new BuildVMM(function).build().start();
                function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
            case CONFIGURING_OR_BUILDING:
                process = new StartHotspot(lambda);
                break;
            case BUILT:
                process = new StartVMM(lambda);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + function.getStatus());
        }

        return process;
    }
}
