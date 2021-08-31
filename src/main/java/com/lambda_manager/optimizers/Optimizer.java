package com.lambda_manager.optimizers;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.processes.lambda.StartLambda;

// TODO - we should try to document interfaces and interface methods.
public interface Optimizer {
    void registerCall(Lambda lambda);
    StartLambda whomToSpawn(Lambda lambda);
}
