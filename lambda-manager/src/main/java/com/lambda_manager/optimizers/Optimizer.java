package com.lambda_manager.optimizers;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.start_lambda.StartLambda;
import com.lambda_manager.utils.LambdaTuple;

public interface Optimizer {
    void registerCall(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration);
    StartLambda whomToSpawn(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration);
}
