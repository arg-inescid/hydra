package com.lambda_manager.schedulers.impl;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.handlers.DefaultLambdaShutdownHandler;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.schedulers.Scheduler;
import com.lambda_manager.core.Environment;
import com.lambda_manager.utils.LambdaTuple;
import com.lambda_manager.utils.Messages;

import java.util.Timer;

@SuppressWarnings("unused")
public class RoundedRobinScheduler implements Scheduler {

    private long previousLambdaStartupTime;

    private void gate(LambdaTuple<Function, Lambda> lambda) {
        long toWait = timeToWait();
        if (toWait > 0) {
            try {
                lambda.function.wait(toWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private long timeToWait() {
        long currentTime = System.currentTimeMillis();
        long timeSpan = currentTime - previousLambdaStartupTime;
        if (timeSpan <= Environment.THRESHOLD) {
            previousLambdaStartupTime += Environment.THRESHOLD;
            return Environment.THRESHOLD - timeSpan;
        } else {
            previousLambdaStartupTime = currentTime;
            return 0;
        }
    }

    private void acquireConnection(LambdaTuple<Function, Lambda> lambda,
                                   LambdaManagerConfiguration configuration) {
        lambda.lambda.setConnectionTriplet(configuration.argumentStorage.nextConnectionTriplet());
    }

    private void buildProcess(LambdaTuple<Function, Lambda> lambda,
                              LambdaManagerConfiguration configuration) {

        ProcessBuilder processBuilder = Processes.START_LAMBDA.build(lambda, configuration);
        lambda.function.getCurrentlyActiveWorkers().put(lambda.lambda.getId(), processBuilder);
        processBuilder.start();
    }

    @Override
    public void spawnNewLambda(LambdaTuple<Function, Lambda> lambda,
                               LambdaManagerConfiguration configuration) {
        gate(lambda);
        acquireConnection(lambda, configuration);
        buildProcess(lambda, configuration);
    }

    @Override
    public LambdaTuple<Function, Lambda> schedule(String functionName, String parameters,
                                                  LambdaManagerConfiguration configuration)
            throws FunctionNotFound {

        Function function = configuration.storage.get(functionName);
        if (function == null) {
            throw new FunctionNotFound(String.format(Messages.FUNCTION_NOT_FOUND, functionName));
        }

        Lambda lambda;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (function) {
            if (function.getStartedLambdas().isEmpty()) {
                if (function.getAvailableLambdas().isEmpty()) {
                    if (function.getActiveLambdas().isEmpty()) {
                        try {
                            function.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        lambda = function.getAvailableLambdas().remove(0);
                        lambda.setParameters(parameters);
                        lambda.shouldUpdateID(function);
                        spawnNewLambda(new LambdaTuple<>(function, lambda), configuration);
                    } else {
                        lambda = function.getActiveLambdas().remove(0);
                    }
                } else {
                    lambda = function.getAvailableLambdas().remove(0);
                    lambda.setParameters(parameters);
                    lambda.shouldUpdateID(function);
                    spawnNewLambda(new LambdaTuple<>(function, lambda), configuration);
                }
            } else {
                lambda = function.getStartedLambdas().remove(0);
                lambda.getTimer().cancel();
            }

            function.getActiveLambdas().add(lambda);
            lambda.setOpenRequestCount(lambda.getOpenRequestCount() + 1);
        }

        return new LambdaTuple<>(function, lambda);
    }

    @Override
    public void reschedule(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        synchronized (lambda.function) {
            int openRequestCount = lambda.lambda.getOpenRequestCount() - 1;
            lambda.lambda.setOpenRequestCount(openRequestCount);
            if (openRequestCount == 0) {
                lambda.function.getActiveLambdas().remove(lambda.lambda);
                lambda.function.getStartedLambdas().add(lambda.lambda);

                Timer currentTimer = lambda.lambda.getTimer();
                if (currentTimer != null) {
                    currentTimer.cancel();
                }
                Timer newTimer = new Timer();
                newTimer.schedule(new DefaultLambdaShutdownHandler(lambda), configuration.argumentStorage.getTimeout());
                lambda.lambda.setTimer(newTimer);
            }
        }
    }
}
