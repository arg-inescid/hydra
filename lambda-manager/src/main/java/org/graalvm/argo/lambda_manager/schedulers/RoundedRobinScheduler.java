package org.graalvm.argo.lambda_manager.schedulers;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.BuildVMM;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspot;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgent;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartTruffle;
import org.graalvm.argo.lambda_manager.processes.lambda.StartVMM;
import org.graalvm.argo.lambda_manager.utils.NetworkUtils;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.core.FunctionLanguage;

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

    private StartLambda whomToSpawn(Lambda lambda, Function function) {
        // TODO - rething this logic.
        StartLambda process;
        switch (function.getStatus()) {
            case NOT_BUILT_NOT_CONFIGURED:
                process = new StartHotspotWithAgent(lambda, function);
                function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
                break;
            case NOT_BUILT_CONFIGURED:
                new BuildVMM(function).build().start();
                function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
            case CONFIGURING_OR_BUILDING:
                process = new StartHotspot(lambda, function);
                break;
            case READY:
                if (function.getLanguage() != FunctionLanguage.NATIVE_JAVA) {
                    process = new StartTruffle(lambda, function);
                } else {
                    process = new StartVMM(lambda, function);
                }
                break;
            // TODO - there might exist a new case here which is for custom runtimes.
            // For these, the start and stop will be through firecracker scripts.
            default:
                throw new IllegalStateException("Unexpected value: " + function.getStatus());
        }
        return process;
    }

    private void startLambda(Function function, Lambda lambda) {
        new Thread() {
            @Override
            public void run() {
                gate();
                lambda.setConnectionTriplet(Configuration.argumentStorage.nextConnectionTriplet());
                ProcessBuilder process = whomToSpawn(lambda, function).build();
                lambda.setProcess(process);
                process.start();
                boolean open = NetworkUtils.waitForOpenPort(lambda.getConnectionTriplet().ip, Configuration.argumentStorage.getLambdaPort(), 25);
                synchronized (function) {
                    if (open) {
                        function.getIdleLambdas().add(lambda);
                        Logger.log(Level.INFO, "Added new lambda for " + function.getName() + " with mode " + lambda.getExecutionMode());
                    } else {
                        Configuration.argumentStorage.deallocateMemoryLambda(function);
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
            if (!lambda.isDecommissioned()) {
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
                    if (Configuration.argumentStorage.allocateMemoryLambda(function)) {
                        Logger.log(Level.INFO, "Requesting new lambda for " + function.getName() + " with mode " + targetMode);
                        lambda = new Lambda(function);
                        startLambda(function, lambda);
                        continue;
                    } else {
                        // TODO - this target mode is a hard rule meaning that we might drastically change the load towards one single VM.
                        if ((lambda = findLambda(function.getRunningLambdas(), targetMode)) != null) {
                            // We remove the lambda so that it can be inserted at the end of the list.
                            function.getRunningLambdas().remove(lambda);
                        }
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
            if (function.getNumberDecommissionedLambdas() == 0) {
                if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT && function.getStatus() == FunctionStatus.READY && !lambda.isDecommissioned()) {
                    function.decommissionLambda(lambda);
                    Logger.log(Level.INFO, "Decommissioning (hotspot to native image) lambda " + lambda.getProcess().pid());
                }

                if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT && lambda.getClosedRequestCount() > 1000) {
                    function.decommissionLambda(lambda);
                    Logger.log(Level.INFO, "Decommissioning (wrapping agent) lambda " + lambda.getProcess().pid());
                }
            }
		}

        return lambda;
    }

    @Override
    public void reschedule(Lambda lambda, Function function) {
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
