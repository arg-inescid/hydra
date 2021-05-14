package com.lambda_manager.handlers;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManager;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.utils.LambdaTuple;

import java.util.TimerTask;

public class DefaultLambdaShutdownHandler extends TimerTask {

    private final LambdaTuple<Function, Lambda> lambda;

    public DefaultLambdaShutdownHandler(LambdaTuple<Function, Lambda> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void run() {
        ProcessBuilder processBuilder;

        synchronized (lambda.function) {
            if(!lambda.function.getStartedLambdas().remove(lambda.lambda)) {
                return;
            }
            lambda.lambda.getTimer().cancel();
            processBuilder = lambda.function.getCurrentlyActiveWorkers().remove(lambda.lambda.getId());
        }

        processBuilder.shutdownInstance();

        // Cleanup is finished, add server back to list of all available servers.
        synchronized (lambda.function) {
            LambdaManager.getConfiguration().argumentStorage.returnConnectionTriplet(lambda.lambda.getConnectionTriplet());
            lambda.function.getAvailableLambdas().add(lambda.lambda);
            lambda.function.notify();
        }
    }
}
