package com.lambda_manager.optimizers;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.processes.lambda.StartLambda;
import com.lambda_manager.utils.LambdaTuple;

public interface Optimizer {
    void registerCall(LambdaTuple<Function, Lambda> lambda);
    StartLambda whomToSpawn(LambdaTuple<Function, Lambda> lambda);
}
