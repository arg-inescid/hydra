package org.graalvm.argo.lambda_manager.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaType;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerPool;

public class LambdaPoolUtils {

    private static final Set<Lambda> startingLambdas = Collections.newSetFromMap(new ConcurrentHashMap<Lambda, Boolean>());

    public static void prepareLambdaPool(Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaManagerPool poolConfiguration) {
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HOTSPOT_W_AGENT, poolConfiguration.getHotspotWithAgent());
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HOTSPOT, poolConfiguration.getHotspot());
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.GRAALVISOR, poolConfiguration.getGraalvisor());
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.CUSTOM, poolConfiguration.getCustom());
    }

    private static void startLambdasPerMode(Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaExecutionMode mode, int amount) {
        for (int i = 0; i < amount; ++i) {
            Lambda lambda = new Lambda(mode);
            // This is a blocking call that waits until the lambda is created.
            startLambda(lambdaPool, lambda, mode);
        }
    }

    public static void startLambda(Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool, Lambda lambda, LambdaExecutionMode targetMode) {
        try {
            startingLambdas.add(lambda);
            long timeBefore = System.currentTimeMillis();
            LambdaConnection connection = Configuration.argumentStorage.getLambdaPool().nextLambdaConnection();
            lambda.setConnection(connection);
            if (Configuration.argumentStorage.getLambdaType().isVM()) {
                NetworkConfigurationUtils.createTap(connection.tap);
            }
            Logger.log(Level.INFO, "Starting new " + targetMode + " lambda.");
            ProcessBuilder process = whomToSpawn(lambda, targetMode).build();
            process.start();
            if (NetworkUtils.waitForOpenPort(lambda.getConnection().ip, lambda.getConnection().port, 25)) {
                lambdaPool.get(targetMode).add(lambda);
                Logger.log(Level.INFO, "Added new lambda with mode " + targetMode + ". It took " + (System.currentTimeMillis() - timeBefore) + " ms.");
            } else {
                new DefaultLambdaShutdownHandler(lambda).run();
                Logger.log(Level.SEVERE, "Failed to add new lambda with mode " + targetMode);
            }
        } catch (Exception e) {
            System.out.println("WARNING!");
            e.printStackTrace();
        } finally {
            startingLambdas.remove(lambda);
        }
    }

    private static StartLambda whomToSpawn(Lambda lambda, LambdaExecutionMode targetMode) {
        switch (targetMode) {
            case HOTSPOT_W_AGENT:
                return Configuration.argumentStorage.getLambdaFactory().createHotspotWithAgent(lambda);
            case HOTSPOT:
                return Configuration.argumentStorage.getLambdaFactory().createHotspot(lambda);
            case GRAALVISOR:
                return Configuration.argumentStorage.getLambdaFactory().createGraalvisor(lambda);
            case CUSTOM:
                return Configuration.argumentStorage.getLambdaFactory().createOpenWhisk(lambda);
            default:
                throw new IllegalStateException("Unexpected value: " + targetMode);
        }
    }

    public static void shutdownLambdas(Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool) {
        // Shutdown lambdas being currently started.
        for (Lambda lambda : startingLambdas) {
            new DefaultLambdaShutdownHandler(lambda).run();
        }
        startingLambdas.clear();
        // Shutdown lambdas from the pool.
        for (Queue<Lambda> queue : lambdaPool.values()) {
            for (Lambda lambda : queue) {
                new DefaultLambdaShutdownHandler(lambda).run();
            }
        }
        lambdaPool.get(LambdaExecutionMode.HOTSPOT_W_AGENT).clear();
        lambdaPool.get(LambdaExecutionMode.HOTSPOT).clear();
        lambdaPool.get(LambdaExecutionMode.GRAALVISOR).clear();
        lambdaPool.get(LambdaExecutionMode.CUSTOM).clear();
    }

    public static void shutdownLambda(Lambda lambda, LambdaType lambdaType) throws InterruptedException {
        try {
            if (lambdaType == LambdaType.CONTAINER || lambdaType == LambdaType.CONTAINER_DEBUG) {
                shutdownContainerLambda(lambda, Environment.CODEBASE + "/" + lambda.getLambdaName());
            } else if (lambdaType == LambdaType.VM_FIRECRACKER || lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT) {
                shutdownFirecrackerLambda(lambda, Environment.CODEBASE + "/" + lambda.getLambdaName(), lambdaType);
            } else if (lambdaType == LambdaType.VM_CONTAINERD) {
                shutdownFirecrackerContainerdLambda(lambda, Environment.CODEBASE + "/" + lambda.getLambdaName());
            } else {
                Logger.log(Level.WARNING, String.format("Lambda ID=%d has no known execution mode: %s", lambda.getLambdaID(), lambda.getExecutionMode()));
            }
        } catch (Throwable t) {
            Logger.log(Level.SEVERE, String.format("Lambda ID=%d failed to shutdown: %s", lambda.getLambdaID(), t.getMessage()));
            t.printStackTrace();
        }

        if (lambdaType.isVM()) {
            NetworkConfigurationUtils.removeTap(lambda.getConnection().tap);
        }
    }

    private static void printStream(Level level, InputStream stream) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(stream));
        String line;

        while ((line = is.readLine()) != null) {
            Logger.log(level, line);
        }
    }

    private static void shutdownFirecrackerContainerdLambda(Lambda lambda, String lambdaPath) throws Throwable {
        String lambdaMode = lambda.getExecutionMode().toString();
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_cruntime.sh", lambdaPath, lambdaMode,
                lambda.getConnection().ip, String.valueOf(lambda.getConnection().port)).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }

    private static void shutdownFirecrackerLambda(Lambda lambda, String lambdaPath, LambdaType lambdaType) throws Throwable {
        String lambdaMode = lambda.getExecutionMode().toString();
        // Append lambda ID to command only if lambda was restored from snapshot (to terminate it properly).
        String lambdaId = lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT ? String.valueOf(lambda.getLambdaID()) : "";
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_firecracker.sh", lambdaPath, lambda.getLambdaName(), lambdaMode,
                lambda.getConnection().ip, String.valueOf(lambda.getConnection().port), lambdaId).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }

    private static void shutdownContainerLambda(Lambda lambda, String lambdaPath) throws Throwable {
        String lambdaMode = lambda.getExecutionMode().toString();
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_container.sh", lambdaPath, lambdaMode,
                lambda.getConnection().ip, String.valueOf(lambda.getConnection().port), lambda.getLambdaName()).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }
}
