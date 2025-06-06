package org.graalvm.argo.lambda_manager.pool;

import org.graalvm.argo.lambda_manager.core.*;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.pool.utils.LambdaPoolUtils;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsOutsidePool;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.utils.NetworkConfigurationUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReactiveLambdaPool implements LambdaPool {

    /**
     * Type of lambdas that will be created.
     */
    private final LambdaType lambdaType;

    /**
     * Maximum number of network taps that will be setup by the Lambda Manager.
     */
    private final int maxLambdas;

    private final String gatewayWithMask;

    /**
     * Concurrent queue where the connections are pooled from.
     */
    private final ConcurrentLinkedQueue<LambdaConnection> connectionPool;

    /**
     * Lambda pool used for amortization of OW/Knative lambda creation.
     */
    private final Map<String, ConcurrentLinkedQueue<Lambda>> lambdaPool;

    /**
     * Locks used to throttle lambda creation - 1 at a time for each function.
     */
    private final Map<String, AtomicBoolean> lambdaCreationLocks;

    /**
     * Used for lambda creation and termination.
     */
    private final ExecutorService executor;

    public ReactiveLambdaPool(LambdaType lambdaType, int maxTaps, String gatewayWithMask) {
        this.lambdaType = lambdaType;
        this.maxLambdas = maxTaps;
        this.gatewayWithMask = gatewayWithMask;
        this.connectionPool = new ConcurrentLinkedQueue<>();
        this.lambdaPool = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(LambdaPoolUtils.EXECUTOR_THREAD_COUNT);
        this.lambdaCreationLocks = new ConcurrentHashMap<>();
    }

    @Override
    public void setUp() {
        int lambdaPort = Configuration.argumentStorage.getLambdaPort();

        if (lambdaType.isVM()) {
            NetworkConfigurationUtils.prepareVmConnectionPool(connectionPool, maxLambdas, gatewayWithMask, lambdaPort);
        } else {
            NetworkConfigurationUtils.prepareContainerConnectionPool(connectionPool, maxLambdas);
        }
    }

    @Override
    public LambdaConnection nextLambdaConnection() {
        return connectionPool.poll();
    }

    @Override
    public Lambda getLambda(LambdaExecutionMode mode, Function function) {
        if (mode.isCustom() /*TODO || mode == LambdaExecutionMode.KNATIVE*/) {
            return pollLambda(mode, function);
        }
        throw new IllegalArgumentException("With a reactive pool, you can only use OpenWhisk or Knative. Mode provided: " + mode);
    }

    @Override
    public void disposeLambda(Lambda lambda) {
        LambdaPoolUtils.shutdownLambda(lambda, lambdaType);
        connectionPool.add(lambda.getConnection());
    }

    @Override
    public void tearDown() throws InterruptedException {
        // Shutdown lambdas inside pool and starting lambdas.
        lambdaPool.values().forEach(LambdaPoolUtils::shutdownLambdas);

        // Close any lasting connection.
        for (LambdaConnection connection : connectionPool) {
            connection.client.close();
        }

        // Delete os-level network interfaces.
        if (lambdaType.isVM()) {
            ProcessBuilder removeTapsOutsidePoolWorker = new RemoveTapsOutsidePool(null).build();
            removeTapsOutsidePoolWorker.start();
            removeTapsOutsidePoolWorker.join();
        }

        // Clearing the connection pool.
        connectionPool.clear();
    }

    private Lambda pollLambda(LambdaExecutionMode mode, Function function) {
        ConcurrentLinkedQueue<Lambda> queue = lambdaPool.computeIfAbsent(function.getName(), k -> new ConcurrentLinkedQueue<>());
        Lambda lambda = queue.poll();
        if (lambda == null) {
            tryStartLambda(mode, function);
        }
        return lambda;
    }

    // TODO: if the lambda gets created but never pooled, it will stay in the 'amortization' data structure until the end.
    private void tryStartLambda(LambdaExecutionMode mode, Function function) {
        String functionName = function.getName();

        // If we are here, it means that we don't have lambdas in the 'amortization' data structure.
        // The only two places where we can have more lambdas - in 'startingLambdas' and in the main set of lambdas.
        if (LambdaManager.lambdas.size() + LambdaPoolUtils.getStartingLambdasCount() >= maxLambdas) {
            // Reclaim some lambdas as we are reaching the max number of lambdas.
            // Use Math.ceil to always reclaim at least one lambda.
            int lambdasToReclaim = (int) Math.ceil(maxLambdas * Configuration.argumentStorage.getReclamationPercentage());
            LambdaManager.lambdas.stream().filter(l -> l.getOpenRequestCount() <= 0).sorted(LambdaPoolUtils::compare)
                    .limit(lambdasToReclaim).forEach(l -> executor.execute(new DefaultLambdaShutdownHandler(l, "reclaiming")));
        }

        AtomicBoolean lock = lambdaCreationLocks.computeIfAbsent(functionName, k -> new AtomicBoolean(false));

        // True -> lock is set, the lambda is already being created. False -> lock is not set, we can create a new lambda.
        if (lock.compareAndSet(false, true)) {
            Lambda newLambda = new Lambda(mode);
            // We only get here after the 'pollLambda' invocation, so the queue should be created by this moment.
            executor.execute(() -> LambdaPoolUtils.startLambda(lambdaPool.get(function.getName()), newLambda, mode, function, lock));
        }
    }
}
