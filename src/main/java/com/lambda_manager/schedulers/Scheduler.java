package com.lambda_manager.schedulers;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.exceptions.user.SchedulingException;
import com.lambda_manager.optimizers.LambdaExecutionMode;

public interface Scheduler {
    Lambda schedule(Function function, LambdaExecutionMode targetMode) throws FunctionNotFound, SchedulingException;

    void reschedule(Lambda lambda);
}
