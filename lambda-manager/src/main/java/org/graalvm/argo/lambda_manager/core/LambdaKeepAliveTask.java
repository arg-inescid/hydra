package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LambdaKeepAliveTask implements Runnable {

    private final ScheduledExecutorService executor;
    private long timeoutPeriodMs;

    public LambdaKeepAliveTask() {
        executor = Executors.newScheduledThreadPool(1);
    }

    public static LambdaKeepAliveTask createAndInit(int timeoutPeriodSeconds) {
        LambdaKeepAliveTask task = new LambdaKeepAliveTask();
        task.timeoutPeriodMs = timeoutPeriodSeconds * 1000L;
        task.executor.scheduleAtFixedRate(task, timeoutPeriodSeconds, timeoutPeriodSeconds, TimeUnit.SECONDS);
        return task;
    }

    @Override
    public void run() {
        long ts = System.currentTimeMillis();
        LambdaManager.lambdas.stream().filter(l -> l.getOpenRequestCount() <= 0 && ts - l.getLastUsedTimestamp() > timeoutPeriodMs)
                .parallel().forEach(l -> new DefaultLambdaShutdownHandler(l, "timer").run());
    }

    public void close() {
        // Gracefully shutdown the executor service.
        executor.shutdown();
        while (true) {
            try {
                if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException ignored) {
            }
        }
    }
}
