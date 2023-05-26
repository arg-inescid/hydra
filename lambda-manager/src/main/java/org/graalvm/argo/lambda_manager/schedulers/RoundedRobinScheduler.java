package org.graalvm.argo.lambda_manager.schedulers;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.core.LambdaType;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.BuildSO;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartOpenWhiskFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorContainer;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.utils.NetworkUtils;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import io.micronaut.http.client.RxHttpClient;

import org.graalvm.argo.lambda_manager.core.FunctionLanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class RoundedRobinScheduler implements Scheduler {

    private long prevLambdaStartTimestamp;

    // Wether a lambda is being decommissioned.
    public static volatile boolean hasDecommissionedLambdas;

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

    private StartLambda whomToSpawn(Lambda lambda, Function function, LambdaExecutionMode targetMode) {
        StartLambda process;
        switch (targetMode) {
            case HOTSPOT_W_AGENT:
                // TODO: exact type of process (VM/Container) should be resolved in a factory.
                if (Configuration.argumentStorage.getLambdaType() == LambdaType.VM) {
                    process = new StartHotspotWithAgentFirecrackerCtr(lambda, function);
                } else {
                    process = new StartHotspotWithAgentContainer(lambda, function);
                }
                break;
            case HOTSPOT:
                if (function.getStatus() == FunctionStatus.NOT_BUILT_CONFIGURED) {
                    new BuildSO(function).build().start();
                    Logger.log(Level.INFO, "Starting new .so build for function " + function.getName());
                }
                if (Configuration.argumentStorage.getLambdaType() == LambdaType.VM) {
                    process = new StartHotspotFirecrackerCtr(lambda, function);
                } else {
                    process = new StartHotspotContainer(lambda, function);
                }
                break;
            case GRAALVISOR:
                if (Configuration.argumentStorage.getLambdaType() == LambdaType.VM) {
                    process = new StartGraalvisorFirecracker(lambda, function);
                } else {
                    process = new StartGraalvisorContainer(lambda, function);
                }
                break;
            case GRAALVISOR_CONTAINERD:
                process = new StartGraalvisorFirecrackerCtr(lambda, function);
                break;
            case CUSTOM:
                process = new StartOpenWhiskFirecrackerCtr(lambda, function);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + function.getStatus());
        }
        Logger.log(Level.INFO, "Starting new " + targetMode + " lambda for function " + function.getName());
        return process;
    }

    private void startLambda(Function function, LambdaExecutionMode targetMode) {
        Lambda lambda = new Lambda(function);
        LambdaManager.startingLambdas.get(targetMode).add(lambda);
        new Thread() {
            @Override
            public void run() {
                try {
                    long timeBefore = System.currentTimeMillis();
                    gate();
                    LambdaConnection connection = Configuration.argumentStorage.nextLambdaConnection();
                    lambda.setConnection(connection);
                    ProcessBuilder process = whomToSpawn(lambda, function, targetMode).build();
                    process.start();
                    if (NetworkUtils.waitForOpenPort(lambda.getConnection().ip, lambda.getConnection().port, 25)) {
                        lambda.resetTimer();
                        LambdaManager.lambdas.add(lambda);
                        Logger.log(Level.INFO, "Added new lambda for " + function.getName() + " with mode " + lambda.getExecutionMode() + ". It took " + (System.currentTimeMillis() - timeBefore) + " ms.");
                    } else {
                        Configuration.argumentStorage.getMemoryPool().deallocateMemoryLambda(function.getMemory());
                        new DefaultLambdaShutdownHandler(lambda).run();
                        Logger.log(Level.SEVERE, "Failed to add new lambda for " + function.getName() + " with mode " + lambda.getExecutionMode());
                    }
                } catch (Exception e) {
                    System.out.println("WARNING!");
                    e.printStackTrace();
                } finally {
                    LambdaManager.startingLambdas.get(targetMode).remove(lambda);
                }
            }
        }.start();
    }

    private Lambda findLambda(Function function, LambdaExecutionMode targetMode, Set<Lambda> lambdas) {
        Lambda result = null;
        for (Lambda lambda : lambdas) {

            // If lambda is decomissioned, not of the correct target, or cannot register this function, skip.
            if (lambda.isDecommissioned() || lambda.getExecutionMode() != targetMode || !lambda.canRegisterInLambda(function)) {
                continue;
            }

            // If we have a runtime with multiple isolates.
            if (function.canCollocateInvocation()) {
                // Acquire memory for a new isolate inside the lambda.
                if (lambda.getMemoryPool().allocateMemoryLambda(function.getMemory())) {
                    // We successfully allocated memory!
                    result = lambda;
                } else {
                    Logger.log(Level.INFO, String.format("[function=%s, mode=%s]: Couldn't allocate memory in lambda %d.", function.getName(), targetMode, lambda.getLambdaID()));
                }
            }
            // If we have a runtime with a single isolate.
            else {
                // If lambda if overloaded, skip.
                if (lambda.getOpenRequestCount() < Environment.LAMBDA_MAX_OPEN_REQ_COUNT) {
                    // Lambda is not overloaded!
                    result = lambda;
                }
            }

            if (result != null) {
                synchronized (lambda) {
                    if (lambda.canRegisterInLambda(function)) {
                        // We won the race.
                        if (!lambda.isRegisteredInLambda(function)) {
                            // Instead of registering function inside synchronized block, we set the flag.
                            lambda.setRequiresFunctionUpload(function);
                            lambda.setRegisteredInLambda(function);
                        }
                        break;
                    } else {
                        // We lost the race, need to revert memory allocation.
                        result = null;
                        if (function.canCollocateInvocation()) {
                            lambda.getMemoryPool().deallocateMemoryLambda(function.getMemory());
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Lambda schedule(Function function, LambdaExecutionMode targetMode) {
        Lambda lambda = null;
        String username = Configuration.coder.decodeUsername(function.getName());

        while (Environment.notShutdownHookActive()) {

            lambda = findLambda(function, targetMode, LambdaManager.lambdas);

            // If we didn't find a lambda, try to allocate a new one.
            if (lambda == null) {
                // This sync block protects from concurrent requests trying to allocate one lambda at the same time.
                synchronized (LambdaManager.lambdas) {
                    // Check if there are lambdas already starting with the execution mode for that user.
                    if (!LambdaManager.startingLambdas.get(targetMode).stream().anyMatch(l -> l.getUsername().equals(username))) {
                        // Acquire memory for a new lambda when needed.
                        if (function.canCollocateInvocation() || Configuration.argumentStorage.getMemoryPool().allocateMemoryLambda(function.getMemory())) {
                            // We successfully allocated memory!
                            startLambda(function, targetMode);
                        } else {
                            Logger.log(Level.FINE, String.format("[function=%s, mode=%s]: No memory available to launch a new lambda, waiting.", function.getName(), targetMode));
                        }
                    } else {
                        Logger.log(Level.FINE, String.format("[function=%s, mode=%s]: Already starting a new lambda, waiting.", function.getName(), targetMode));
                    }
                }
            } else {
                // Cancel the lambda timeout timer.
                lambda.getTimer().cancel(); // TODO - can we avoid this? It may have a sync...
                // Increment the open request count.
                lambda.incOpenRequestCount();
                // Break the while true.
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignored.
            }
        }

        synchronized (function) {
            // We only one lambda to be decommissioned at a time.
            if (!hasDecommissionedLambdas) {
                if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT && function.getStatus() == FunctionStatus.READY && !lambda.isDecommissioned()) {
                    hasDecommissionedLambdas = true;
                    lambda.setDecommissioned(true);
                    Logger.log(Level.INFO, "Decommissioning (hotspot to native image) lambda " + lambda.getLambdaID());
                }

                if (lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT && lambda.getClosedRequestCount() > 1000 && !lambda.isDecommissioned()) {
                    hasDecommissionedLambdas = true;
                    lambda.setDecommissioned(true);
                    Logger.log(Level.INFO, "Decommissioning (wrapping agent) lambda " + lambda.getLambdaID());
                }
            }
        }

        Logger.log(Level.INFO, String.format("Found lambda %d.", lambda.getLambdaID()));
        return lambda;
    }

    @Override
    public void reschedule(Lambda lambda, Function function) {
        synchronized (function) {
            int openRequestCount = lambda.decOpenRequestCount();
            lambda.incClosedRequestCount();
            if (openRequestCount == 0) {
                lambda.resetTimer();
            }
        }
        if (function.canCollocateInvocation()) {
            lambda.getMemoryPool().deallocateMemoryLambda(function.getMemory());
        }
    }
}
