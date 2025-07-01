package org.graalvm.argo.lambda_manager.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.utils.logger.ElapseTimer;

public class MetricsProvider {

    private static final AtomicInteger completedRequests = new AtomicInteger(0);
    private static final AtomicInteger cinv = new AtomicInteger(0);

    private static final String METRIC_RECORD = "{\"timestamp\":%d, \"system_footprint\":%.3f, "
            + "\"user_cpu\":%.3f, \"system_cpu\":%.3f, \"graalos_memory\":%.3f,"
            + "\"open_requests\":%d, \"active_lambdas\":%d, \"active_lambdas_running\":%d, "
            + "\"active_users\":%d, \"throughput\":%d, \"cinv\":%d, "
            + "\"lambdas_memory_pool\":[%s], \"graalos_individual_memory\":[%s]}";

    private static final String LAMBDA_OBJECT = "{\"name\":\"%s\",\"running\":%d},";

    public static String getMetricsRecord() {
        long timestamp = ElapseTimer.elapsedTime();

        double systemFootprint = LambdaMetricsUtils.collectSystemFootprint();
        double[] cpus = LambdaMetricsUtils.collectCpuNumbers();
        double userCpu = cpus[0];
        double systemCpu = cpus[1];
        List<Double> graalosFootprints = LambdaMetricsUtils.collectGraalOSFootprint();
        String individualGraalosFootprints = graalosFootprints.stream().map(String::valueOf).collect(Collectors.joining(","));
        double totalGraalosFootprint = graalosFootprints.stream().mapToDouble(Double::doubleValue).sum();
        int lambdasRunning = 0;
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

            sb.append(String.format(LAMBDA_OBJECT, lambda.getLambdaName(), lambdaOpenRequests));
        }
        sb.setLength(Math.max(sb.length() - 1, 0)); // To remove the last comma.

        String result = String.format(METRIC_RECORD, timestamp, systemFootprint, userCpu, systemCpu, totalGraalosFootprint, openRequests, LambdaManager.lambdas.size(),
                lambdasRunning, activeUsers.size(), completedRequests.get(), cinv.get(), sb.toString(), individualGraalosFootprints);

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
