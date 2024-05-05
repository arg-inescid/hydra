package org.graalvm.argo.lambda_manager.utils;

import java.io.BufferedReader;
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
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.core.LambdaType;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerPool;

public class LambdaPoolUtils {

    private static final Set<Lambda> startingLambdas = Collections.newSetFromMap(new ConcurrentHashMap<Lambda, Boolean>());

    private static final int EXECUTOR_THREAD_COUNT = Runtime.getRuntime().availableProcessors() / 2;

    private static final int DAEMON_SLEEP_PERIOD_MS = 5000;
    /**
     * Only reclaim lambdas that were used more than "LRU_RECLAMATION_PERIOD_MS" ms ago.
     */
    private static final int LRU_RECLAMATION_PERIOD_MS = 5000;
    private static final float LAMBDA_RECLAIM_THRESHOLD = 0.3f;
    private static final float LAMBDA_PERCENTAGE_TO_RECLAIM = LAMBDA_RECLAIM_THRESHOLD * 0.5f;

    public static void prepareLambdaPool(Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaManagerPool poolConfiguration) {
        ExecutorService executor = Executors.newFixedThreadPool(EXECUTOR_THREAD_COUNT);

        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HOTSPOT_W_AGENT, poolConfiguration.getHotspotWithAgent(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.HOTSPOT, poolConfiguration.getHotspot(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.GRAALVISOR, poolConfiguration.getGraalvisor(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.CUSTOM_JAVA, poolConfiguration.getCustomJava(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.CUSTOM_JAVASCRIPT, poolConfiguration.getCustomJavaScript(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.CUSTOM_PYTHON, poolConfiguration.getCustomPython(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.GRAALVISOR_PGO, poolConfiguration.getGraalvisorPgo(), executor);
        startLambdasPerMode(lambdaPool, LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED, poolConfiguration.getGraalvisorPgoOptimized(), executor);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private static void startLambdasPerMode(Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaExecutionMode mode, int amount, ExecutorService executor) {
        for (int i = 0; i < amount; ++i) {
            executor.execute(() -> {
                Lambda lambda = new Lambda(mode);
                // This is a blocking call that waits until the lambda is created.
                startLambda(lambdaPool, lambda, mode);
            });
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
            // TODO: create entries in factory.
            case GRAALVISOR_PGO:
                return Configuration.argumentStorage.getLambdaFactory().createGraalvisorPgo(lambda);
            case GRAALVISOR_PGO_OPTIMIZED:
                return Configuration.argumentStorage.getLambdaFactory().createGraalvisorPgoOptimized(lambda);
            case CUSTOM_JAVA:
            case CUSTOM_JAVASCRIPT:
            case CUSTOM_PYTHON:
                return Configuration.argumentStorage.getLambdaFactory().createOpenWhisk(lambda);
            default:
                throw new IllegalStateException("Unexpected value: " + targetMode);
        }
    }

    public static void shutdownLambdas(Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool) {
        ExecutorService executor = Executors.newFixedThreadPool(EXECUTOR_THREAD_COUNT);
        // Shutdown lambdas being currently started.
        for (Lambda lambda : startingLambdas) {
            executor.execute(new DefaultLambdaShutdownHandler(lambda)::run);
        }
        startingLambdas.clear();
        // Shutdown lambdas from the pool.
        for (Queue<Lambda> queue : lambdaPool.values()) {
            for (Lambda lambda : queue) {
                executor.execute(new DefaultLambdaShutdownHandler(lambda)::run);
            }
        }
        lambdaPool.get(LambdaExecutionMode.HOTSPOT_W_AGENT).clear();
        lambdaPool.get(LambdaExecutionMode.HOTSPOT).clear();
        lambdaPool.get(LambdaExecutionMode.GRAALVISOR).clear();
        lambdaPool.get(LambdaExecutionMode.CUSTOM_JAVA).clear();
        lambdaPool.get(LambdaExecutionMode.CUSTOM_JAVASCRIPT).clear();
        lambdaPool.get(LambdaExecutionMode.CUSTOM_PYTHON).clear();
        lambdaPool.get(LambdaExecutionMode.GRAALVISOR_PGO).clear();
        lambdaPool.get(LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED).clear();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
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

    public static void startLambdaReclaimingDaemon(Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool, LambdaManagerPool poolConfiguration) {
        Runnable task = new LambdaReclaimingDaemon(poolConfiguration, lambdaPool);
        Thread daemon = new Thread(task);
        daemon.start();
    }

    private static class LambdaReclaimingDaemon implements Runnable {

        private final Map<LambdaExecutionMode, Integer> maxLambdas;
        private final Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool;

        private LambdaReclaimingDaemon(LambdaManagerPool poolConfiguration, Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool) {
            this.maxLambdas = new HashMap<>();
            this.maxLambdas.put(LambdaExecutionMode.HOTSPOT_W_AGENT, poolConfiguration.getHotspotWithAgent());
            this.maxLambdas.put(LambdaExecutionMode.HOTSPOT, poolConfiguration.getHotspot());
            this.maxLambdas.put(LambdaExecutionMode.GRAALVISOR, poolConfiguration.getGraalvisor());
            this.maxLambdas.put(LambdaExecutionMode.CUSTOM_JAVA, poolConfiguration.getCustomJava());
            this.maxLambdas.put(LambdaExecutionMode.CUSTOM_JAVASCRIPT, poolConfiguration.getCustomJavaScript());
            this.maxLambdas.put(LambdaExecutionMode.CUSTOM_PYTHON, poolConfiguration.getCustomPython());
            this.maxLambdas.put(LambdaExecutionMode.GRAALVISOR_PGO, poolConfiguration.getGraalvisorPgo());
            this.maxLambdas.put(LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED, poolConfiguration.getGraalvisorPgoOptimized());
            this.lambdaPool = lambdaPool;
        }

        @Override
        public void run() {
            while (Environment.notShutdownHookActive()) {
                try {
                    lambdaPool.forEach(this::reclaim);
                    Thread.sleep(DAEMON_SLEEP_PERIOD_MS);
                } catch (InterruptedException ie) {
                    // Ignore.
                } catch (Throwable th) {
                    Logger.log(Level.WARNING, String.format("A problem occurred during the lambda reclaiming process: %s", th.getMessage()));
                }
            }
        }

        private void reclaim(LambdaExecutionMode mode, ConcurrentLinkedQueue<Lambda> lambdas) {
            int total = maxLambdas.get(mode);
            int minimalThreshold = (int) (total * LAMBDA_RECLAIM_THRESHOLD);
            int lambdasInPool = lambdas.size();
            if (lambdasInPool < minimalThreshold) {
                // Use Math.ceil to always reclaim at least one lambda.
                int lambdasToReclaim = (int) Math.ceil(total * LAMBDA_PERCENTAGE_TO_RECLAIM);
                long ts = System.currentTimeMillis();
                LambdaManager.lambdas.stream().filter(l -> l.getExecutionMode() == mode && l.getOpenRequestCount() <= 0 && ts - l.getTimerTimestamp() > LRU_RECLAMATION_PERIOD_MS).sorted(this::compare)
                        .limit(lambdasToReclaim).parallel().forEach(l -> new DefaultLambdaShutdownHandler(l).run());
            }
        }

        private int compare(Lambda l1, Lambda l2) {
            return (int) (l1.getTimerTimestamp() - l2.getTimerTimestamp());
        }
    }
}
