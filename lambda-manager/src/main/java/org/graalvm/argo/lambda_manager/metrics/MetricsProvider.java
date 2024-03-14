package org.graalvm.argo.lambda_manager.metrics;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.utils.logger.ElapseTimer;

public class MetricsProvider {

    private static final AtomicInteger completedRequests = new AtomicInteger(0);
    private static final AtomicInteger cinv = new AtomicInteger(0);

    private static final String METRIC_RECORD = "{\"timestamp\":%d, \"system_footprint\":%.3f, "
            + "\"open_requests\":%d, \"active_lambdas\":%d, \"active_lambdas_running\":%d, "
            + "\"lambda_pool_lambdas\":%d, \"active_users\":%d, \"throughput\":%d, \"cinv\":%d, "
            + "\"lambdas_memory_pool\":[%s]},\n";

    private static final String LAMBDA_OBJECT = "{\"name\":\"%s\",\"pool_free\":%d,\"running\":%d},";

    public static String getMetricsRecord() {
        long timestamp = ElapseTimer.elapsedTime();

        double systemFootprint = LambdaMemoryUtils.collectSystemFootprint();
        int lambdasRunning = 0;
        int lambdaPoolLambdas = 0;
        int openRequests = 0;
        Set<String> activeUsers = new HashSet<>();
        StringBuilder sb = new StringBuilder();

        for (Lambda lambda : LambdaManager.lambdas) {
            int lambdaOpenRequests = lambda.getOpenRequestCount();
            if (lambdaOpenRequests > 0) {
                openRequests += lambdaOpenRequests;
                ++lambdasRunning;
            }
            activeUsers.add(lambda.getUsername());

            sb.append(String.format(LAMBDA_OBJECT, lambda.getLambdaName(), lambda.getMemoryPool().getFreeMemory(), lambdaOpenRequests));
        }
        for (ConcurrentLinkedQueue<Lambda> lambdas : Configuration.argumentStorage.getLambdaPool().lambdaPool.values()) {
            lambdaPoolLambdas += lambdas.size();
        }
        sb.setLength(Math.max(sb.length() - 1, 0)); // To remove the last comma.

        String result = String.format(METRIC_RECORD, timestamp, systemFootprint, openRequests, LambdaManager.lambdas.size(),
                lambdasRunning, lambdaPoolLambdas, activeUsers.size(), completedRequests.get(), cinv.get(), sb.toString());

        completedRequests.set(0);
        return result;
    }

    public static void addRequest() {
        completedRequests.incrementAndGet();
    }

    public static void addConcurrentRequest() {
        cinv.incrementAndGet();
    }

    public static void removeConcurrentRequest() {
        cinv.decrementAndGet();
    }
}
