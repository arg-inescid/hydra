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
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartOpenWhiskFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotFirecracker;
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

    // Wether a lambda is being decommissioned.
    public static volatile boolean hasDecommissionedLambdas;

    private Lambda findLambda(Function function, LambdaExecutionMode targetMode, Set<Lambda> lambdas) {
        for (Lambda lambda : lambdas) {

            boolean hotSpotMatch = targetMode == LambdaExecutionMode.HOTSPOT && lambda.getExecutionMode() == LambdaExecutionMode.HOTSPOT_W_AGENT;
            boolean modeMatch = targetMode == lambda.getExecutionMode() || hotSpotMatch;
            boolean lambdaOverloaded = !function.canCollocateInvocation() && lambda.getOpenRequestCount() >= Environment.LAMBDA_MAX_OPEN_REQ_COUNT;
            // If lambda is decomissioned, not of the correct target, overloaded, or cannot register this function, skip.
            if (lambda.isDecommissioned() || !modeMatch || lambdaOverloaded || !lambda.canRegisterInLambda(function)) {
                continue;
            }

            if (lambda.tryRegisterInLambda(function)) {
                return lambda;
            }
        }
        return null;
    }

    @Override
    public Lambda schedule(Function function, LambdaExecutionMode targetMode) {
        Lambda lambda = null;
        String username = Configuration.coder.decodeUsername(function.getName());
        boolean obtainedLambda = false;

        while (Environment.notShutdownHookActive()) {

            lambda = findLambda(function, targetMode, LambdaManager.lambdas);

            // If we didn't find a lambda, try to get a new one from the pool.
            if (lambda == null) {
                Lambda newLambda = Configuration.argumentStorage.getLambdaPool().getLambda(targetMode);
                if (newLambda != null) {
                    newLambda.resetTimer();
                    LambdaManager.lambdas.add(newLambda);
                    function.updateStatus(targetMode);
                    obtainedLambda = true;
                } else {
                    Logger.log(Level.FINE, String.format("[function=%s, mode=%s]: The lambda pool is currently empty, waiting.", function.getName(), targetMode));
                }
            } else {
                // Cancel the lambda timeout timer and increment the open request count.
                lambda.incOpenRequests();
                // Break the while true.
                break;
            }

            if (!obtainedLambda) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignored.
                }
            }
            obtainedLambda = false;
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
        // Decrement the open request count and reset the lambda timeout timer.
        lambda.decOpenRequests();
        if (function.canCollocateInvocation()) {
            lambda.getMemoryPool().deallocateMemoryLambda(function.getMemory());
        }
    }
}
