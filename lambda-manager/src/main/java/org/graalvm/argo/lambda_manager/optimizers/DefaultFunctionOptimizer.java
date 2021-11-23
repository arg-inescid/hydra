package org.graalvm.argo.lambda_manager.optimizers;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.BuildVMM;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspot;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgent;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartVMM;

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
