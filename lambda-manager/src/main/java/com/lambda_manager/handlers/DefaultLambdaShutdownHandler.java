package com.lambda_manager.handlers;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.utils.Tuple;

import java.util.TimerTask;

public class DefaultLambdaShutdownHandler extends TimerTask {

    private final Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;

    public DefaultLambdaShutdownHandler(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
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
            lambda.list.getAvailableInstances().add(lambda.instance);
            lambda.list.notify();
        }
    }
}
