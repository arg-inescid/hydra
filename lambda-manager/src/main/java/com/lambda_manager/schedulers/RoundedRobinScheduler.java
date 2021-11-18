package com.lambda_manager.schedulers;

import com.lambda_manager.core.Configuration;
import com.lambda_manager.core.Environment;
import com.lambda_manager.core.Function;
import com.lambda_manager.core.Lambda;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.utils.NetworkUtils;
import com.lambda_manager.utils.logger.Logger;
import java.util.ArrayList;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class RoundedRobinScheduler implements Scheduler {

    private long prevLambdaStartTimestamp;

    private synchronized void gate() {
        try {
            Thread.sleep(timeToWait());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private long timeToWait() {
        long currentTime = System.currentTimeMillis();
        long timeSpan = currentTime - prevLambdaStartTimestamp;
        if (timeSpan <= Environment.LAMBDA_STARTUP_THRESHOLD) {
            prevLambdaStartTimestamp += Environment.LAMBDA_STARTUP_THRESHOLD;
            return Environment.LAMBDA_STARTUP_THRESHOLD - timeSpan;
        } else {
            prevLambdaStartTimestamp = currentTime;
            return 0;
        }
    }

    private void startLambda(Function function, Lambda lambda) {
        new Thread() {
            @Override
            public void run() {
                gate();
                lambda.setConnectionTriplet(Configuration.argumentStorage.nextConnectionTriplet());
                ProcessBuilder process = Configuration.optimizer.whomToSpawn(lambda).build();
                lambda.setProcess(process);
                process.start();
                boolean open = NetworkUtils.waitForOpenPort(lambda.getConnectionTriplet().ip, Configuration.argumentStorage.getLambdaPort(), 25);
                synchronized (function) {
                    if (open) {
                        function.getIdleLambdas().add(lambda);
                        Logger.log(Level.INFO, "Added new lambda for " + function.getName() + " with mode " + lambda.getExecutionMode());
                    } else {
                        function.getStoppedLambdas().add(lambda);
                        Logger.log(Level.SEVERE, "Failed to add new lambda for " + function.getName() + " with mode " + lambda.getExecutionMode());
                    }
                    lambda.resetTimer();
                }
            }
        }.start();
    }

    private Lambda findLambda(ArrayList<Lambda> lambdas) {
        return findLambda(lambdas, null);
    }

    private Lambda findLambda(ArrayList<Lambda> lambdas, LambdaExecutionMode targetMode) {
        for (Lambda lambda : lambdas) {
            if (!lambda.isDecomissioned()) {
                if (targetMode == null || lambda.getExecutionMode() == targetMode) {
                    return lambda;
                }
            }
        }
        return null;
    }

    @Override
    public Lambda schedule(Function function, LambdaExecutionMode targetMode) {
        Lambda lambda;

        while (true) {

            synchronized (function) {
                if ((lambda = findLambda(function.getIdleLambdas(), targetMode)) == null) {
                    if ((lambda = findLambda(function.getStoppedLambdas())) == null) {
                        // TODO - this target mode is a hard rule meaning that we might drastically change the load towards one single VM.
                        // We should slowly transition the load to the VMs with target load.
                        if ((lambda = findLambda(function.getRunningLambdas(), targetMode)) != null) {
                            // We remove the lambda so that it can be inserted at the end of the list.
                            function.getRunningLambdas().remove(lambda);
                        }
                    } else {
                        // We remove from stopped and start the lambda in the background.
                        function.getStoppedLambdas().remove(lambda);
                        Logger.log(Level.INFO, "Requesting new lambda for " + function.getName() + " with mode " + targetMode);
                        startLambda(function, lambda);
                        continue;
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
                Logger.log(Level.WARNING, "No suitable Lambda to execute request for " + function.getName() + " with mode " + targetMode);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignored.
            }
        }

        synchronized (function) {
            // We only one lambda to be decommissioned at a time.
            if (function.getNumberDecommissedLambdas() == 0) {
                if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT && function.getStatus() == FunctionStatus.BUILT && !lambda.isDecomissioned()) {
                    function.decommissionLambda(lambda);
                    Logger.log(Level.INFO, "Decommisioning (hotspot to native image) lambda " + lambda.getProcess().pid());
                }

                if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT && lambda.getClosedRequestCount() > 1000) {
                    function.decommissionLambda(lambda);
                    Logger.log(Level.INFO, "Decommisioning (wrapping agent) lambda " + lambda.getProcess().pid());
                }
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
                lambda.resetTimer();
            }
        }
    }
}
