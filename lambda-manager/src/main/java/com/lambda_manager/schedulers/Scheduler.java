package com.lambda_manager.schedulers;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.exceptions.user.SchedulingException;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.utils.LambdaTuple;

public interface Scheduler {
    LambdaTuple<Function, Lambda> schedule(Function function, LambdaExecutionMode targetMode) throws FunctionNotFound, SchedulingException;
    void reschedule(LambdaTuple<Function, Lambda> lambda);
}
