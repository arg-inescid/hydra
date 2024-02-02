package org.graalvm.argo.lambda_manager.metrics;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.utils.Buffer;
import org.graalvm.argo.lambda_manager.utils.logger.ElapseTimer;

public class MetricsProvider {

    private static Buffer requestBuffer = Buffer.create();
    private static Buffer infrastructureBuffer = Buffer.create();

    private static final String METRIC_RECORD = "{\"timestamp\":%d, \"system_footprint\":%.3f, \"system_footprint_running\":%.3f, "
            + "\"lambda_pool_footprint\":%.3f, \"request_latency_max\":%d, \"request_latency_avg\":%.3f, "
            + "\"infrastructure_latency_max\":%d, \"infrastructure_latency_avg\":%.3f, \"open_requests\":%d, "
            + "\"active_lambdas\":%d, \"active_lambdas_running\":%d, \"lambda_pool_lambdas\":%d, \"active_users\":%d, "
            + "\"throughput\":%d, \"lambdas_memory_pool\":[%s]},\n";

    private static final String LAMBDA_OBJECT = "{\"name\":\"%s\",\"footprint\":%.3f,\"pool_free\":%d,\"running\":%d},";

    public static String getMetricsRecord() {
        long timestamp = ElapseTimer.elapsedTime();

        double systemFootprint = 0;
        double systemFootprintRunning = 0;
        double lambdaPoolFootprint = 0;
        int lambdasRunning = 0;
        int lambdaPoolLambdas = 0;
        int openRequests = 0;
        Set<String> activeUsers = new HashSet<>(128);
        Map<String, Double> memoryTraces;
        if (Configuration.argumentStorage.getLambdaType().isVM()) {
            memoryTraces = LambdaMemoryUtils.collectMemoryMetricsFirecracker();
        } else {
            memoryTraces = LambdaMemoryUtils.collectMemoryMetricsContainer();
        }
        StringBuilder sb = new StringBuilder();

        for (Lambda lambda : LambdaManager.lambdas) {
            double lambdaFootprint = 0.0;
            if (Configuration.argumentStorage.getLambdaType().isVM()) {
                lambdaFootprint = memoryTraces.getOrDefault(lambda.getConnection().tap, 0.0);
            } else {
                lambdaFootprint = memoryTraces.getOrDefault(lambda.getLambdaName(), 0.0);
            }
            systemFootprint += lambdaFootprint;
            int lambdaOpenRequests = lambda.getOpenRequestCount();
            if (lambdaOpenRequests > 0) {
                openRequests += lambdaOpenRequests;
                systemFootprintRunning += lambdaFootprint;
                ++lambdasRunning;
            }
            activeUsers.add(lambda.getUsername());

            sb.append(String.format(LAMBDA_OBJECT, lambda.getLambdaName(), lambdaFootprint, lambda.getMemoryPool().getFreeMemory(), lambdaOpenRequests));
        }
        for (ConcurrentLinkedQueue<Lambda> lambdas : Configuration.argumentStorage.getLambdaPool().lambdaPool.values()) {
            for (Lambda lambda : lambdas) {
                if (Configuration.argumentStorage.getLambdaType().isVM()) {
                    lambdaPoolFootprint += memoryTraces.getOrDefault(lambda.getConnection().tap, 0.0);
                } else {
                    lambdaPoolFootprint += memoryTraces.getOrDefault(lambda.getLambdaName(), 0.0);
                }
                ++lambdaPoolLambdas;
            }
        }
        sb.setLength(Math.max(sb.length() - 1, 0)); // To remove the last comma.

        String result = String.format(METRIC_RECORD, timestamp, systemFootprint, systemFootprintRunning, lambdaPoolFootprint, requestBuffer.max(),
                requestBuffer.avg(), infrastructureBuffer.max(), infrastructureBuffer.avg(), openRequests,
                LambdaManager.lambdas.size(), lambdasRunning, lambdaPoolLambdas, activeUsers.size(), requestBuffer.size(), sb.toString());

        requestBuffer.reset();
        infrastructureBuffer.reset();
        return result;
    }

    public static void reportInfrastructureTime(long time) {
        infrastructureBuffer.offer(time);
    }

    public static void reportRequestTime(long time) {
        requestBuffer.offer(time);
    }

}
