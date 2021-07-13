package com.lambda_manager.schedulers.impl;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.core.Environment;
import com.lambda_manager.exceptions.user.FunctionNotFound;
import com.lambda_manager.exceptions.user.SchedulingException;
import com.lambda_manager.handlers.DefaultLambdaShutdownHandler;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.processes.lambda.StartLambda;
import com.lambda_manager.schedulers.Scheduler;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;

import java.util.ArrayList;
import java.util.Timer;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class RoundedRobinScheduler implements Scheduler {

    private long previousLambdaStartupTime;

    private void gate(Function function) {
        long toWait = timeToWait();
        if (toWait > 0) {
            try {
                function.wait(toWait);
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

    private void acquireConnection(Lambda lambda) {
        lambda.setConnectionTriplet(Configuration.argumentStorage.nextConnectionTriplet());
    }

    private void spawnNewLambda(Function function, Lambda lambda) {
        gate(function);
        acquireConnection(lambda);
        lambda.setFunction(function);
        ProcessBuilder process = Configuration.optimizer.whomToSpawn(lambda).build();
        lambda.setProcess(process);
        function.addProcess(process);
        process.start();
    }

    private Lambda findLambda(ArrayList<Lambda> lambdas) {
        return findLambda(lambdas, null);
    }

    private Lambda findLambda(ArrayList<Lambda> lambdas, LambdaExecutionMode targetMode) {
        for (Lambda lambda : lambdas) {
            if (!lambda.isDecomissioned()) {
                 if (targetMode != null && lambda.getExecutionMode() != targetMode) {
                     continue;
                 } else {
                     return lambda;
                 }
            }
        }
        return null;
    }

    @Override
    public Lambda schedule(Function function, LambdaExecutionMode targetMode) throws FunctionNotFound, SchedulingException {
        Lambda lambda = null;

        while (true) {

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (function) {
                if ((lambda = findLambda(function.getIdleLambdas(), targetMode)) == null) {
                    if ((lambda = findLambda(function.getStoppedLambdas())) == null) {
                        if ((lambda = findLambda(function.getRunningLambdas(), targetMode)) != null) {
                            function.getRunningLambdas().remove(lambda);
                        }
                    } else {
                        function.getStoppedLambdas().remove(lambda);
                        spawnNewLambda(function, lambda);
                    }
                } else {
                    function.getIdleLambdas().remove(lambda);
                    lambda.getTimer().cancel();
                }
                if (lambda != null) {
                    function.getRunningLambdas().add(lambda);
                    lambda.incOpenRequestCount();
                    break;
                }
            }

            try {
                // TODO - print a warning.
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // TODO - move to Configuration.optimizer -> should be here?
        if (function.getNumberDecommissedLambdas() < (function.getTotalNumberLambdas() / 2)) {
            if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT && function.getStatus() == FunctionStatus.BUILT && !lambda.isDecomissioned()) {
                function.decommissionLambda(lambda);
                Logger.log(Level.INFO, "Decommisioning (hotspot to native image) lambda " + lambda.getProcess().pid());
            }

            if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT && lambda.getClosedRequestCount() > 1000) {
                function.decommissionLambda(lambda);
                Logger.log(Level.INFO, "Decommisioning (wrapping agent) lambda " + lambda.getProcess().pid());
            }
        }

        return lambda;
    }

    @Override
    public void reschedule(Lambda lambda) {
        Function function = lambda.getFunction();
        synchronized (function) {
            int openRequestCount = lambda.decOpenRequestCount();
            lambda.incClosedRequestCount();
            if (openRequestCount == 0) {
                function.getRunningLambdas().remove(lambda);
                function.getIdleLambdas().add(lambda);

                Timer currentTimer = lambda.getTimer();
                if (currentTimer != null) {
                    currentTimer.cancel();
                }
                Timer newTimer = new Timer();
                // TODO - allow a timer for immediate termination.
                newTimer.schedule(new DefaultLambdaShutdownHandler(lambda), Configuration.argumentStorage.getTimeout());
                lambda.setTimer(newTimer);
            }
        }
    }
}
