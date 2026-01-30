package org.graalvm.argo.lambda_manager.pool.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.*;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.NetworkConfigurationUtils;
import org.graalvm.argo.lambda_manager.utils.NetworkConnection;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerPool;

// TODO (rbruno): this class should be merged with LambdaPool.
public class LambdaPoolUtils {

    private static final Set<Lambda> startingLambdas = Collections.newSetFromMap(new ConcurrentHashMap<Lambda, Boolean>());

    public static final int EXECUTOR_THREAD_COUNT = Runtime.getRuntime().availableProcessors() / 2;

    public static void prepareLambdaPool(Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaManagerPool poolConfiguration) {
        ExecutorService executor = Executors.newFixedThreadPool(EXECUTOR_THREAD_COUNT);

        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HOTSPOT_W_AGENT, poolConfiguration.getHotspotWithAgent(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HOTSPOT, poolConfiguration.getHotspot(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HYDRA, poolConfiguration.getHydra(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.GRAALOS, poolConfiguration.getGraalOS(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.CUSTOM_JAVA, poolConfiguration.getCustomJava(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.CUSTOM_JAVASCRIPT, poolConfiguration.getCustomJavaScript(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.CUSTOM_PYTHON, poolConfiguration.getCustomPython(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HYDRA_PGO, poolConfiguration.getHydraPgo(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HYDRA_PGO_OPTIMIZED, poolConfiguration.getHydraPgoOptimized(), executor);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private static void startLambdasPerMode(Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaExecutionMode mode, int amount, ExecutorService executor) {
        for (int i = 0; i < amount; ++i) {
            executor.execute(() -> {
                Lambda lambda = new Lambda(mode);
                // This is a blocking call that waits until the lambda is created.
                startLambda(lambdaPool.get(mode.name()), lambda, mode, null, null);
            });
        }
    }

    public static void startLambda(Queue<Lambda> lambdaPool, Lambda lambda, LambdaExecutionMode targetMode, Function function, AtomicBoolean lambdaCreationLock) {
        try {
            startingLambdas.add(lambda);
            long timeBefore = System.currentTimeMillis();
            LambdaConnection connection = Configuration.argumentStorage.getLambdaPool().nextLambdaConnection();
            lambda.setConnection(connection);
            if (Configuration.argumentStorage.getLambdaType().isVM()) {
                NetworkConfigurationUtils.createTap(((NetworkConnection) connection).getTap());
            }
            Logger.log(Level.INFO, "Starting new " + targetMode + " lambda.");
            ProcessBuilder process = whomToSpawn(lambda, targetMode, function).build();
            process.start();
            if (connection.waitUntilReady()) {
                lambdaPool.add(lambda);
                Logger.log(Level.INFO, "Added new lambda with mode " + targetMode + ". It took " + (System.currentTimeMillis() - timeBefore) + " ms.");
            } else {
                new DefaultLambdaShutdownHandler(lambda, "failed to add").run();
                Logger.log(Level.SEVERE, "Failed to add new lambda with mode " + targetMode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            startingLambdas.remove(lambda);
            if (lambdaCreationLock != null) {
                lambdaCreationLock.set(false);
            }
        }
    }

    private static StartLambda whomToSpawn(Lambda lambda, LambdaExecutionMode targetMode, Function function) {
        switch (targetMode) {
            case HOTSPOT_W_AGENT:
                return Configuration.argumentStorage.getLambdaFactory().createHotspotWithAgent(lambda);
            case HOTSPOT:
                return Configuration.argumentStorage.getLambdaFactory().createHotspot(lambda);
            case HYDRA:
                return Configuration.argumentStorage.getLambdaFactory().createHydra(lambda);
            case HYDRA_PGO:
                return Configuration.argumentStorage.getLambdaFactory().createHydraPgo(lambda);
            case HYDRA_PGO_OPTIMIZED:
                return Configuration.argumentStorage.getLambdaFactory().createHydraPgoOptimized(lambda);
            case GRAALOS:
                return Configuration.argumentStorage.getLambdaFactory().createGraalOS(lambda);
            case CUSTOM_JAVA:
            case CUSTOM_JAVASCRIPT:
            case CUSTOM_PYTHON:
                return Configuration.argumentStorage.getLambdaFactory().createOpenWhisk(lambda, function);
            case KNATIVE:
                return Configuration.argumentStorage.getLambdaFactory().createKnative(lambda, function);
            default:
                throw new IllegalStateException("Unexpected value: " + targetMode);
        }
    }

    public static void shutdownLambdas(Queue<Lambda> lambdaPool) {
        ExecutorService executor = Executors.newFixedThreadPool(EXECUTOR_THREAD_COUNT);
        // Shutdown lambdas being currently started.
        for (Lambda lambda : startingLambdas) {
            executor.execute(new DefaultLambdaShutdownHandler(lambda, "pool tear down (starting)"));
        }
        startingLambdas.clear();
        // Shutdown lambdas from the pool.
        for (Lambda lambda : lambdaPool) {
            executor.execute(new DefaultLambdaShutdownHandler(lambda, "pool tear down (from the pool)"));
        }
        lambdaPool.clear();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public static boolean shutdownLambda(Lambda lambda, LambdaType lambdaType) {
        boolean success;
        try {
            if (lambdaType == LambdaType.CONTAINER || lambdaType == LambdaType.CONTAINER_DEBUG) {
                success = shutdownContainerLambda(lambda, Environment.CODEBASE + "/" + lambda.getLambdaName());
            } else if (lambdaType == LambdaType.VM_FIRECRACKER || lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT) {
                success = shutdownFirecrackerLambda(lambda, Environment.CODEBASE + "/" + lambda.getLambdaName(), lambdaType);
            } else if (lambdaType == LambdaType.GRAALOS_NATIVE) {
                success = shutdownNativeLambda(lambda, Environment.CODEBASE + "/" + lambda.getLambdaName());
            } else {
                Logger.log(Level.WARNING, String.format("Lambda ID=%d has no known execution mode: %s", lambda.getLambdaID(), lambda.getExecutionMode()));
                success = false;
            }
        } catch (Throwable t) {
            Logger.log(Level.SEVERE, String.format("Lambda ID=%d failed to shutdown: %s", lambda.getLambdaID(), t.getMessage()));
            t.printStackTrace();
            success = false;
        }

        if (lambdaType.isVM()) {
            try {
                NetworkConfigurationUtils.removeTap(((NetworkConnection)lambda.getConnection()).getTap());
            } catch (InterruptedException e) {
                Logger.log(Level.WARNING, Messages.ERROR_TAP_REMOVAL, e);
            }
        }
        return success;
    }

    private static void printStream(Level level, InputStream stream) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(stream));
        String line;

        while ((line = is.readLine()) != null) {
            Logger.log(level, line);
        }
    }

    private static boolean shutdownFirecrackerLambda(Lambda lambda, String lambdaPath, LambdaType lambdaType) throws Throwable {
        String lambdaMode = lambda.getExecutionMode().toString();
        NetworkConnection conn = (NetworkConnection) lambda.getConnection();
        // Append lambda ID to command only if lambda was restored from snapshot (to terminate it properly).
        String lambdaId = lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT ? String.valueOf(lambda.getLambdaID()) : "";
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_firecracker.sh", lambdaPath, lambda.getLambdaName(), lambdaMode,
                conn.getIp(), String.valueOf(conn.getPort()), lambdaId).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
            return false;
        }
        return true;
    }

    private static boolean shutdownContainerLambda(Lambda lambda, String lambdaPath) throws Throwable {
        String lambdaMode = lambda.getExecutionMode().toString();
        NetworkConnection conn = (NetworkConnection) lambda.getConnection();
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_container.sh", lambdaPath, lambdaMode,
                conn.getIp(), String.valueOf(conn.getPort()), lambda.getLambdaName()).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
            return false;
        }
        return true;
    }

    // TODO - will have to fix.
    private static boolean shutdownNativeLambda(Lambda lambda, String lambdaPath) throws Throwable {
        File f = new File(Environment.LAMBDA_LOGS + "/" + lambda.getLambdaName() + "/terminate.log");
        NetworkConnection conn = (NetworkConnection) lambda.getConnection();
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_graalos_native.sh", lambdaPath, String.valueOf(conn.getPort())).redirectOutput(f).redirectError(f).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
            return false;
        }
        return true;
    }

    public static void startLambdaReclaimingDaemon(Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaManagerPool poolConfiguration) {
        Runnable task = new LambdaReclaimingDaemon(poolConfiguration, lambdaPool);
        Thread daemon = new Thread(task);
        daemon.start();
    }

    private static class LambdaReclaimingDaemon implements Runnable {

        private final Map<String, Integer> maxLambdas;
        private final Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool;

        private LambdaReclaimingDaemon(LambdaManagerPool poolConfiguration, Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool) {
            this.maxLambdas = new HashMap<>();
            this.maxLambdas.put(LambdaExecutionMode.HOTSPOT_W_AGENT.name(), poolConfiguration.getHotspotWithAgent());
            this.maxLambdas.put(LambdaExecutionMode.HOTSPOT.name(), poolConfiguration.getHotspot());
            this.maxLambdas.put(LambdaExecutionMode.HYDRA.name(), poolConfiguration.getHydra());
            this.maxLambdas.put(LambdaExecutionMode.GRAALOS.name(), poolConfiguration.getHydra());
            this.maxLambdas.put(LambdaExecutionMode.CUSTOM_JAVA.name(), poolConfiguration.getCustomJava());
            this.maxLambdas.put(LambdaExecutionMode.CUSTOM_JAVASCRIPT.name(), poolConfiguration.getCustomJavaScript());
            this.maxLambdas.put(LambdaExecutionMode.CUSTOM_PYTHON.name(), poolConfiguration.getCustomPython());
            this.maxLambdas.put(LambdaExecutionMode.HYDRA_PGO.name(), poolConfiguration.getHydraPgo());
            this.maxLambdas.put(LambdaExecutionMode.HYDRA_PGO_OPTIMIZED.name(), poolConfiguration.getHydraPgoOptimized());
            this.lambdaPool = lambdaPool;
        }

        @Override
        public void run() {
            while (Environment.notShutdownHookActive()) {
                try {
                    lambdaPool.forEach(this::reclaim);
                    Thread.sleep(Configuration.argumentStorage.getReclamationInterval());
                } catch (InterruptedException ie) {
                    // Ignore.
                } catch (Throwable th) {
                    Logger.log(Level.WARNING, String.format("A problem occurred during the lambda reclaiming process: %s", th.getMessage()));
                }
            }
        }

        private void reclaim(String mode, ConcurrentLinkedQueue<Lambda> lambdas) {
            int total = maxLambdas.get(mode);
            int minimalThreshold = (int) (total * Configuration.argumentStorage.getReclamationThreshold());
            int lambdasInPool = lambdas.size();
            if (lambdasInPool < minimalThreshold) {
                // Use Math.ceil to always reclaim at least one lambda.
                int lambdasToReclaim = (int) Math.ceil(total * Configuration.argumentStorage.getReclamationPercentage());
                long ts = System.currentTimeMillis();
                LambdaManager.lambdas.stream().filter(l -> l.getExecutionMode().name().equals(mode) && l.getOpenRequestCount() <= 0 && ts - l.getLastUsedTimestamp() > Configuration.argumentStorage.getLruReclamationPeriod()).sorted(LambdaPoolUtils::compare)
                        .limit(lambdasToReclaim).parallel().forEach(l -> new DefaultLambdaShutdownHandler(l, "reclaiming").run());
            }
        }
    }

    public static int compare(Lambda l1, Lambda l2) {
        return (int) (l1.getLastUsedTimestamp() - l2.getLastUsedTimestamp());
    }
}
