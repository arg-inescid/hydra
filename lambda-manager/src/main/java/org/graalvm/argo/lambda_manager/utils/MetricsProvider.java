package org.graalvm.argo.lambda_manager.utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;

import io.reactivex.Single;

/**
 * MetricsProvider is specific to the Prometheus metrics format
 */
public class MetricsProvider {

    private static Buffer requestBuffer = Buffer.create();
    private static Buffer coldStartBuffer = Buffer.create();

    private static final String LAMBDA_FOOTPRINT = "lambda_footprint{name=\"%s\"} %.3f %d\n";
    private static final String SYSTEM_FOOTPRINT = "system_footprint %.3f %d\n";
    private static final String REQUEST_LATENCY_MAX = "request_latency_max %d %d\n";
    private static final String REQUEST_LATENCY_AVG = "request_latency_avg %.3f %d\n";
    private static final String COLD_START_LATENCY_MAX = "cold_start_latency_max %d %d\n";
    private static final String COLD_START_LATENCY_AVG = "cold_start_latency_avg %.3f %d\n";

    private static final String OPEN_REQUESTS = "open_requests %d %d\n";
    private static final String ACTIVE_LAMBDAS = "active_lambdas %d %d\n";
    private static final String ACTIVE_USERS = "active_users %d %d\n";

    private static final String THROUGHPUT= "throughput %d %d\n";

    public static Single<String> getFootprintAndScalability() {
        long timestamp = System.currentTimeMillis();
        StringBuilder responseBuilder = new StringBuilder();

        double totalMemory = 0;
        int openRequests = 0;
        Set<String> activeUsers = new HashSet<>(64);
        Map<String, Double> memoryTraces = LambdaMemoryUtils.collectMemoryMetrics();
        for (Lambda lambda : LambdaManager.lambdas) {
            double lambdaMemory = memoryTraces.getOrDefault(lambda.getCustomRuntimeId(), 0.0);
            totalMemory += lambdaMemory;
            openRequests += lambda.getOpenRequestCount();
            responseBuilder.append(String.format(LAMBDA_FOOTPRINT, lambda.getLambdaName(), lambdaMemory, timestamp));
            activeUsers.add(lambda.getUsername());
        }

        responseBuilder.append(String.format(SYSTEM_FOOTPRINT, totalMemory, timestamp));
        responseBuilder.append(String.format(REQUEST_LATENCY_MAX, requestBuffer.max(), timestamp));
        responseBuilder.append(String.format(REQUEST_LATENCY_AVG, requestBuffer.avg(), timestamp));
        responseBuilder.append(String.format(COLD_START_LATENCY_MAX, coldStartBuffer.max(), timestamp));
        responseBuilder.append(String.format(COLD_START_LATENCY_AVG, coldStartBuffer.avg(), timestamp));
        responseBuilder.append(String.format(OPEN_REQUESTS, openRequests, timestamp));
        responseBuilder.append(String.format(ACTIVE_LAMBDAS, LambdaManager.lambdas.size(), timestamp));
        responseBuilder.append(String.format(ACTIVE_USERS, activeUsers.size(), timestamp));
        responseBuilder.append(String.format(THROUGHPUT, requestBuffer.size(), timestamp));
        requestBuffer.reset();
        coldStartBuffer.reset();
        return Single.just(responseBuilder.toString());
    }

    public static void reportColdStartTime(long time) {
        coldStartBuffer.offer(time);
    }

    public static void reportRequestTime(long time) {
        requestBuffer.offer(time);
    }

}
