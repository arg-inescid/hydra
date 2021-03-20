package com.lambda_manager.schedulers;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.LambdaNotFound;
import com.lambda_manager.utils.Tuple;

public interface Scheduler {
    Tuple<LambdaInstancesInfo, LambdaInstanceInfo> schedule(String lambdaName, String args, LambdaManagerConfiguration configuration) throws LambdaNotFound;
    void reschedule(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);
    void spawnNewLambda(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);
}
