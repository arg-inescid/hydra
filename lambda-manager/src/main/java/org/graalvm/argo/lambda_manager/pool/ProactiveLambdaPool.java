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

public class ProactiveLambdaPool extends LambdaPool {

    // TODO - add documentation.
    private final LambdaManagerPool poolConfiguration;

    public ProactiveLambdaPool(LambdaType lambdaType, int maxTaps, LambdaManagerPool poolConfiguration) {
        super(lambdaType, maxTaps);
        this.poolConfiguration = poolConfiguration;
    }

    @Override
    public void setUp() {
        int lambdaPort = Configuration.argumentStorage.getLambdaPort();

        this.lambdaPool.putAll(Map.ofEntries(
            Map.entry(LambdaExecutionMode.HOTSPOT_W_AGENT.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.HOTSPOT.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.HYDRA.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.GRAALOS.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_JAVA.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_JAVASCRIPT.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.CUSTOM_PYTHON.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.HYDRA_PGO.name(), new ConcurrentLinkedQueue<>()),
            Map.entry(LambdaExecutionMode.HYDRA_PGO_OPTIMIZED.name(), new ConcurrentLinkedQueue<>())));

        if (lambdaType.isVM()) {
            String gatewayWithMask = Configuration.argumentStorage.getGatewayWithMask();
            NetworkConfigurationUtils.prepareVmConnectionPool(connectionPool, maxLambdas, gatewayWithMask, lambdaPort);
        } else {
            NetworkConfigurationUtils.prepareContainerConnectionPool(connectionPool, maxLambdas);
        }

        LambdaPoolUtils.prepareLambdaPool(lambdaPool, poolConfiguration);
        LambdaPoolUtils.startLambdaReclaimingDaemon(lambdaPool, poolConfiguration);
    }

    @Override
    public Lambda getLambda(LambdaExecutionMode mode, Function function) {
        return lambdaPool.get(mode.name()).poll();
    }

    /**
     * Dispose the used lambda and replenish the pool with a new lambda.
     */
    @Override
    public void disposeLambda(Lambda lambda) {
        if (lambda.isIntact() && Environment.notShutdownHookActive()) {
            // The lambda was not used, we can add it to the pool right away.
            lambdaPool.get(lambda.getExecutionMode().name()).add(lambda);
        } else {
            boolean success = LambdaPoolUtils.shutdownLambda(lambda, lambdaType);
            connectionPool.add(lambda.getConnection());
            // To avoid deadlock when new lambdas are forcefully terminated and created again.
            // Only replenish if managed to terminate the previous lambda successfully.
            if (success && Environment.notShutdownHookActive()) {
                Lambda newLambda = new Lambda(lambda.getExecutionMode());
                LambdaPoolUtils.startLambda(lambdaPool.get(lambda.getExecutionMode().name()), newLambda, lambda.getExecutionMode(), null, null);
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
