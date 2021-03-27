package com.lambda_manager.schedulers.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.LambdaNotFound;
import com.lambda_manager.handlers.DefaultLambdaShutdownHandler;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.schedulers.Scheduler;
import com.lambda_manager.utils.Tuple;

import java.util.Timer;

@SuppressWarnings("unused")
public class RoundedRobinScheduler implements Scheduler {

    @Override
    public synchronized Tuple<LambdaInstancesInfo, LambdaInstanceInfo> schedule(String lambdaName, String args, LambdaManagerConfiguration configuration) throws LambdaNotFound {
        LambdaInstancesInfo lambdaInstancesInfo = configuration.storage.get(lambdaName);
        if(lambdaInstancesInfo == null) {
            throw new LambdaNotFound("Lambda [" + lambdaName + "] has not been uploaded!");
        }
        LambdaInstanceInfo lambdaInstanceInfo;

        if(lambdaInstancesInfo.getStartedInstances().isEmpty()) {
            if(lambdaInstancesInfo.getAvailableInstances().isEmpty()) {
                lambdaInstanceInfo = lambdaInstancesInfo.getActiveInstances().remove(0);
            } else {
                lambdaInstanceInfo = lambdaInstancesInfo.getAvailableInstances().remove(0);
                lambdaInstanceInfo.setArgs(args);
                spawnNewLambda(new Tuple<>(lambdaInstancesInfo, lambdaInstanceInfo), configuration);
            }
        } else {
            lambdaInstanceInfo = lambdaInstancesInfo.getStartedInstances().remove(0);
            lambdaInstanceInfo.getTimer().cancel();
            lambdaInstanceInfo.setTimer(new Timer());
        }

        lambdaInstancesInfo.getActiveInstances().add(lambdaInstanceInfo);
        lambdaInstanceInfo.setOpenRequestCount(lambdaInstanceInfo.getOpenRequestCount() + 1);
        return new Tuple<>(lambdaInstancesInfo, lambdaInstanceInfo);
    }

    @Override
    public synchronized void reschedule(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        int openRequestCount = lambda.instance.getOpenRequestCount() - 1;
        lambda.instance.setOpenRequestCount(openRequestCount);
        if(openRequestCount == 0) {
            lambda.list.getActiveInstances().remove(lambda.instance);
            lambda.list.getStartedInstances().add(lambda.instance);
            lambda.instance.getTimer().schedule(new DefaultLambdaShutdownHandler(lambda), configuration.argumentStorage.getTimeout());
        }
    }

    @Override
    public void spawnNewLambda(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        Tuple<String, String> tapIp = configuration.argumentStorage.getTapIp();
        lambda.instance.setTap(tapIp.list);
        lambda.instance.setIp(tapIp.instance);

        lambda.instance.setTimer(new Timer());
        ProcessBuilder processBuilder = Processes.START_LAMBDA.build(lambda, configuration);
        lambda.list.getCurrentlyActiveWorkers().put(lambda.instance.getId(), processBuilder);
        processBuilder.start();
    }
}
