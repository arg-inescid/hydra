package com.lambda_manager.handlers;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.utils.Tuple;

import java.util.TimerTask;

public class DefaultLambdaShutdownHandler extends TimerTask {

    private final Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;

    public DefaultLambdaShutdownHandler(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void run() {
        lambda.instance.getTimer().cancel();

        synchronized (lambda.list) {
            lambda.list.getCurrentlyActiveWorkers().remove(lambda.instance.getId()).shutdownInstance();
            lambda.list.getStartedInstances().remove(lambda.instance);
        }

        // Cleanup is finished, add server back to list of all available servers.
        synchronized (lambda.list) {
            lambda.list.getAvailableInstances().add(lambda.instance);
        }
    }
}
