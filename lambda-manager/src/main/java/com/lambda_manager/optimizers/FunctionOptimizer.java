package com.lambda_manager.optimizers;

import com.lambda_manager.core.Lambda;
import com.lambda_manager.processes.lambda.StartLambda;


/**
 * Tracks the state of a Function and triggers optimizations if necessary.
 */
public interface FunctionOptimizer {

	/**
	 * Tracks invocations of a function.
	 * @param lambda
	 */
    default void registerCall(Lambda lambda) {
    }

    /**
     * Decides which mode of execution should be used for the invocation.
     * @param lambda
     * @return
     */
    StartLambda whomToSpawn(Lambda lambda);
}
