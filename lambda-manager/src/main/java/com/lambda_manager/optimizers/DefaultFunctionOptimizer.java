package com.lambda_manager.optimizers;

import com.lambda_manager.core.Function;
import com.lambda_manager.core.Lambda;
import com.lambda_manager.processes.lambda.BuildVMM;
import com.lambda_manager.processes.lambda.StartHotspot;
import com.lambda_manager.processes.lambda.StartHotspotWithAgent;
import com.lambda_manager.processes.lambda.StartLambda;
import com.lambda_manager.processes.lambda.StartVMM;

public class DefaultFunctionOptimizer implements FunctionOptimizer {

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
