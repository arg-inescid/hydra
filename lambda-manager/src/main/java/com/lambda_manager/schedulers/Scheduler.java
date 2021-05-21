package com.lambda_manager.schedulers;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.utils.LambdaTuple;

public interface Scheduler {
    LambdaTuple<Function, Lambda> schedule(String lambdaName, String args) throws FunctionNotFound;
    void reschedule(LambdaTuple<Function, Lambda> lambda);
    void spawnNewLambda(LambdaTuple<Function, Lambda> lambda);
}
