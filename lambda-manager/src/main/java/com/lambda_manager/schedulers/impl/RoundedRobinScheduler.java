package com.lambda_manager.schedulers.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.LambdaNotFound;
import com.lambda_manager.handlers.DefaultLambdaShutdownHandler;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.schedulers.Scheduler;
import com.lambda_manager.utils.LambdaTuple;

import java.util.Timer;

@SuppressWarnings("unused")
public class RoundedRobinScheduler implements Scheduler {

    private static final int THRESHOLD = 200;

    private long previousLambdaStartupTime;

    private void gate(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
        long toWait = timeToWait();
        if (toWait > 0) {
            try {
                lambda.list.wait(toWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private long timeToWait() {
        long currentTime = System.currentTimeMillis();
        long timeSpan = currentTime - previousLambdaStartupTime;
        if (timeSpan <= THRESHOLD) {
            previousLambdaStartupTime += THRESHOLD;
            return THRESHOLD - timeSpan;
        } else {
            previousLambdaStartupTime = currentTime;
            return 0;
        }
    }

    private void acquireConnection(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                                   LambdaManagerConfiguration configuration) {
        lambda.instance.setConnectionTriplet(configuration.argumentStorage.nextConnectionTriplet());
    }

    private void buildProcess(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                              LambdaManagerConfiguration configuration) {

        ProcessBuilder processBuilder = Processes.START_LAMBDA.build(lambda, configuration);
        lambda.list.getCurrentlyActiveWorkers().put(lambda.instance.getId(), processBuilder);
        processBuilder.start();
    }

    @Override
    public void spawnNewLambda(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                               LambdaManagerConfiguration configuration) {
        gate(lambda);
        acquireConnection(lambda, configuration);
        buildProcess(lambda, configuration);
    }

    @Override
    public LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> schedule(String lambdaName, String args,
                                                                         LambdaManagerConfiguration configuration)
            throws LambdaNotFound {

        LambdaInstancesInfo lambdaInstancesInfo = configuration.storage.get(lambdaName);
        if (lambdaInstancesInfo == null) {
            throw new LambdaNotFound("Lambda [" + lambdaName + "] has not been uploaded!");
        }

        LambdaInstanceInfo lambdaInstanceInfo;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lambdaInstancesInfo) {
            if (lambdaInstancesInfo.getStartedInstances().isEmpty()) {
                if (lambdaInstancesInfo.getAvailableInstances().isEmpty()) {
                    if (lambdaInstancesInfo.getActiveInstances().isEmpty()) {
                        try {
                            lambdaInstancesInfo.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        lambdaInstanceInfo = lambdaInstancesInfo.getAvailableInstances().remove(0);
                        lambdaInstanceInfo.setArgs(args);
                        lambdaInstanceInfo.shouldUpdateID(lambdaInstancesInfo);
                        spawnNewLambda(new LambdaTuple<>(lambdaInstancesInfo, lambdaInstanceInfo), configuration);
                    } else {
                        lambdaInstanceInfo = lambdaInstancesInfo.getActiveInstances().remove(0);
                    }
                } else {
                    lambdaInstanceInfo = lambdaInstancesInfo.getAvailableInstances().remove(0);
                    lambdaInstanceInfo.setArgs(args);
                    lambdaInstanceInfo.shouldUpdateID(lambdaInstancesInfo);
                    spawnNewLambda(new LambdaTuple<>(lambdaInstancesInfo, lambdaInstanceInfo), configuration);
                }
            } else {
                lambdaInstanceInfo = lambdaInstancesInfo.getStartedInstances().remove(0);
                lambdaInstanceInfo.getTimer().cancel();
            }

            lambdaInstancesInfo.getActiveInstances().add(lambdaInstanceInfo);
            lambdaInstanceInfo.setOpenRequestCount(lambdaInstanceInfo.getOpenRequestCount() + 1);
        }

        return new LambdaTuple<>(lambdaInstancesInfo, lambdaInstanceInfo);
    }

    @Override
    public void reschedule(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        synchronized (lambda.list) {
            int openRequestCount = lambda.instance.getOpenRequestCount() - 1;
            lambda.instance.setOpenRequestCount(openRequestCount);
            if (openRequestCount == 0) {
                lambda.list.getActiveInstances().remove(lambda.instance);
                lambda.list.getStartedInstances().add(lambda.instance);

                Timer currentTimer = lambda.instance.getTimer();
                if (currentTimer != null) {
                    currentTimer.cancel();
                }
                Timer newTimer = new Timer();
                newTimer.schedule(new DefaultLambdaShutdownHandler(lambda), configuration.argumentStorage.getTimeout());
                lambda.instance.setTimer(newTimer);
            }
        }
    }
}
