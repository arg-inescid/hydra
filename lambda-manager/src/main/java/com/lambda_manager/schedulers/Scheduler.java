package com.lambda_manager.schedulers;

import com.lambda_manager.core.Function;
import com.lambda_manager.core.Lambda;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.exceptions.user.SchedulingException;
import com.lambda_manager.optimizers.LambdaExecutionMode;

/**
 * The scheduler decides where (in which Lambda) to execution a function invocation.
 */
public interface Scheduler {

	/**
	 * Returns the Lambda that should host the function invocation and that respects the given target mode.
	 * @param function
	 * @param targetMode
	 * @return
	 * @throws FunctionNotFound
	 * @throws SchedulingException
	 */
    Lambda schedule(Function function, LambdaExecutionMode targetMode) throws FunctionNotFound, SchedulingException;

    /**
     * Reschedules the Lambda after executing the lambda invocation.
     * @param lambda
     */
    void reschedule(Lambda lambda);
}
