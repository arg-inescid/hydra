package org.graalvm.argo.lambda_manager.core;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.taps.CreateTaps;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsFromPool;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsOutsidePool;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.utils.NetworkConfigurationUtils;

import io.micronaut.context.BeanContext;

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
     * Concurrent queue where the connections are poolled from.
     */
    private final ConcurrentLinkedQueue<LambdaConnection> connectionPool;

    public LambdaPool(LambdaType lambdaType, int maxTaps) {
        this.lambdaType = lambdaType;
        this.targetSize = maxTaps;
        this.connectionPool = new ConcurrentLinkedQueue<>();
    }

    public void setUp(BeanContext beanContext, int lambdaPort, String gatewayWithMask) throws InterruptedException {
        if (lambdaType.isVM()) {
            NetworkConfigurationUtils.prepareVmConnectionPool(connectionPool, targetSize, gatewayWithMask, lambdaPort, beanContext);

            // Create os-level network interfaces (taps).
            ProcessBuilder createTaps = new CreateTaps(connectionPool).build();
            createTaps.start();
            createTaps.join();
        } else {
            NetworkConfigurationUtils.prepareContainerConnectionPool(connectionPool, targetSize, beanContext);
        } 
    }

    public LambdaConnection nextLambdaConnection() {
        return connectionPool.poll();
    }

    public void returnLambdaConnection(LambdaConnection connection) {
        connectionPool.add(connection);
    }

    public void tearDown() throws InterruptedException {
        // Close any lasting connection.
        for (LambdaConnection connection : connectionPool) {
            connection.client.close();   // Close http client if it's not closed yet.
        }

        // Delete os-level network interfaces.
        if (lambdaType.isVM()) {
            ProcessBuilder removeTapsWorker = new RemoveTapsFromPool(connectionPool).build();
            removeTapsWorker.start();
            removeTapsWorker.join();

            ProcessBuilder removeTapsOutsidePoolWorker = new RemoveTapsOutsidePool(connectionPool).build();
            removeTapsOutsidePoolWorker.start();
            removeTapsOutsidePoolWorker.join();
        }

        // Clearing the connection pool.
        connectionPool.clear();
    }
}
