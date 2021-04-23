package com.lambda_manager.handlers;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManager;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.utils.LambdaTuple;

import java.util.TimerTask;

public class DefaultLambdaShutdownHandler extends TimerTask {

    private final LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;

    public DefaultLambdaShutdownHandler(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void run() {
        ProcessBuilder processBuilder;

        synchronized (lambda.list) {
            if(!lambda.list.getStartedInstances().remove(lambda.instance)) {
                return;
            }
            lambda.instance.getTimer().cancel();
            processBuilder = lambda.list.getCurrentlyActiveWorkers().remove(lambda.instance.getId());
        }

        processBuilder.shutdownInstance();

        // Cleanup is finished, add server back to list of all available servers.
        synchronized (lambda.list) {
            LambdaManager.getConfiguration().argumentStorage.returnConnectionTriplet(lambda.instance.getConnectionTriplet());
            lambda.list.getAvailableInstances().add(lambda.instance);
            lambda.list.notify();
        }
    }
}
