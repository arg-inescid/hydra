package org.graalvm.argo.lambda_manager.pool;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaType;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsOutsidePool;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.pool.utils.LambdaPoolUtils;
import org.graalvm.argo.lambda_manager.utils.NetworkConfigurationUtils;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerPool;

public class ProactiveLambdaPool implements LambdaPool {

    /**
     * Type of lambdas that will be created.
     */
    private final LambdaType lambdaType;

    /**
     * Maximum number of network taps that will be setup by the Lambda Manager.
     */
    private final int maxLambdas;

    private final String gatewayWithMask;

    private final LambdaManagerPool poolConfiguration;

    /**
     * Concurrent queue where the connections are pooled from.
     */
    private final ConcurrentLinkedQueue<LambdaConnection> connectionPool;

    /**
     * Concurrent queue where the lambdas are pooled from.
     */
    private final Map<LambdaExecutionMode, ConcurrentLinkedQueue<Lambda>> lambdaPool = Map.ofEntries(
            Map.entry(LambdaExecutionMode.HOTSPOT_W_AGENT, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.HOTSPOT, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALVISOR, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALOS, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_JAVA, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_JAVASCRIPT, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_PYTHON, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALVISOR_PGO, new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED, new ConcurrentLinkedQueue<>()));

    public ProactiveLambdaPool(LambdaType lambdaType, int maxTaps, String gatewayWithMask, LambdaManagerPool poolConfiguration) {
        this.lambdaType = lambdaType;
        this.maxLambdas = maxTaps;
        this.gatewayWithMask = gatewayWithMask;
        this.poolConfiguration = poolConfiguration;
        this.connectionPool = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void setUp() {
        int lambdaPort = Configuration.argumentStorage.getLambdaPort();

        if (lambdaType.isVM()) {
            NetworkConfigurationUtils.prepareVmConnectionPool(connectionPool, maxLambdas, gatewayWithMask, lambdaPort);
        } else {
            NetworkConfigurationUtils.prepareContainerConnectionPool(connectionPool, maxLambdas);
        }
        LambdaPoolUtils.prepareLambdaPool(lambdaPool, poolConfiguration);
        LambdaPoolUtils.startLambdaReclaimingDaemon(lambdaPool, poolConfiguration);
    }

    @Override
    public LambdaConnection nextLambdaConnection() {
        return connectionPool.poll();
    }

    @Override
    public Lambda getLambda(LambdaExecutionMode mode, Function function) {
        return lambdaPool.get(mode).poll();
    }

    /**
     * Dispose the used lambda and replenish the pool with a new lambda.
     */
    @Override
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
                LambdaPoolUtils.startLambda(lambdaPool.get(lambda.getExecutionMode()), newLambda, lambda.getExecutionMode(), null, null);
            }
        }
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
}
