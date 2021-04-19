package com.lambda_manager.schedulers;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.LambdaNotFound;
import com.lambda_manager.utils.LambdaTuple;

public interface Scheduler {
    LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> schedule(String lambdaName, String args, LambdaManagerConfiguration configuration) throws LambdaNotFound;
    void reschedule(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);
    void spawnNewLambda(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);
}
