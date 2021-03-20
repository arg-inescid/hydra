package com.serverless_demo.schedulers;

import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.exceptions.user.LambdaNotFound;
import com.serverless_demo.utils.Tuple;

public interface Scheduler {
    Tuple<LambdaInstancesInfo, LambdaInstanceInfo> schedule(String lambdaName, String args, LambdaManagerConfiguration configuration) throws LambdaNotFound;
    void reschedule(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);
    void spawnNewLambda(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);
}
