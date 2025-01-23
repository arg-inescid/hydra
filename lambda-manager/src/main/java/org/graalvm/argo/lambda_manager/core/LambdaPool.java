package org.graalvm.argo.lambda_manager.core;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsOutsidePool;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.utils.LambdaPoolUtils;
import org.graalvm.argo.lambda_manager.utils.NetworkConfigurationUtils;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerPool;

public class LambdaPool {

    /**
     * Type of lambdas that will be created.
     */
    private final LambdaType lambdaType;

    /**
     * Maximum number of network taps that will be setup by the Lambda Manager.
     */
    private final int targetSize;

    /**
     * Concurrent queue where the connections are pooled from.
     */
    private final ConcurrentLinkedQueue<LambdaConnection> connectionPool;

    /**
     * Concurrent queue where the lambdas are pooled from.
     */
    public final Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool = Map.ofEntries(
            Map.entry(LambdaExecutionMode.HOTSPOT_W_AGENT, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.HOTSPOT, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALVISOR, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALOS, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_JAVA, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_JAVASCRIPT, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_PYTHON, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALVISOR_PGO, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED, new ConcurrentLinkedQueue<>()));

    public LambdaPool(LambdaType lambdaType, int maxTaps) {
        this.lambdaType = lambdaType;
        this.targetSize = maxTaps;
        this.connectionPool = new ConcurrentLinkedQueue<>();
    }

    public void setUp(int lambdaPort, String gatewayWithMask, LambdaManagerPool poolConfiguration) {
        if (lambdaType.isVM()) {
            NetworkConfigurationUtils.prepareVmConnectionPool(connectionPool, targetSize, gatewayWithMask, lambdaPort);
        } else {
            NetworkConfigurationUtils.prepareContainerConnectionPool(connectionPool, targetSize);
        }
        LambdaPoolUtils.prepareLambdaPool(lambdaPool, poolConfiguration);
        LambdaPoolUtils.startLambdaReclaimingDaemon(lambdaPool, poolConfiguration);
    }

    public LambdaConnection nextLambdaConnection() {
        return connectionPool.poll();
    }

    public Lambda getLambda(LambdaExecutionMode mode) {
        return lambdaPool.get(mode).poll();
    }

    /**
     * Dispose the used lambda and replenish the pool with a new lambda.
     */
    public void disposeLambda(Lambda lambda) {
        if (lambda.isIntact() && Environment.notShutdownHookActive()) {
            // The lambda was not used, we can add it to the pool right away.
            lambdaPool.get(lambda.getExecutionMode()).add(lambda);
        } else {
            boolean success = LambdaPoolUtils.shutdownLambda(lambda, lambdaType);
            connectionPool.add(lambda.getConnection());
            // To avoid deadlock when new lambdas are forcefully terminated and created again.
            // Only replenish if managed to terminate the previous lambda successfully.
            if (success && Environment.notShutdownHookActive()) {
                Lambda newLambda = new Lambda(lambda.getExecutionMode());
                LambdaPoolUtils.startLambda(lambdaPool, newLambda, lambda.getExecutionMode());
            }
        }
    }

    public void tearDown() throws InterruptedException {
        // Shutdown lambdas inside pool and starting lambdas.
        LambdaPoolUtils.shutdownLambdas(lambdaPool);

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
}
