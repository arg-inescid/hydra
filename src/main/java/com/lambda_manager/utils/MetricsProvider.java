package com.lambda_manager.utils;

import java.util.Map;

import com.lambda_manager.core.Configuration;
import com.lambda_manager.core.Function;
import com.lambda_manager.core.Lambda;

import io.reactivex.Single;

/**
 * MetricsProvider is specific to the Prometheus metrics format
 */
public class MetricsProvider {

    private static Buffer buffer = Buffer.create();

    private static final String FOOTPRINT_METRIC = "function_footprint{user=\"%s\",function=\"%s\"} %.3f %d\n";
    private static final String SCALABILITY_METRIC = "function_scalability{user=\"%s\",function=\"%s\"} %d %d\n";
    private static final String LATENCY_METRIC = "request_latency{user=\"%s\",function=\"%s\"} %d %d\n";


    public static Single<String> getFootprintAndScalability() {
        try {
            Map<String, Function> functionsMap = Configuration.storage.getAll();
            StringBuilder responseBuilder = new StringBuilder();
            for (Map.Entry<String, Function> entry : functionsMap.entrySet()) {
                Function function = entry.getValue();
                double functionFootprint = 0;
                synchronized (function) {
                    for (Lambda lambda : function.getRunningLambdas()) {
                        functionFootprint += LambdaMemoryUtils.getProcessMemory(function, lambda);
                    }
                    for (Lambda lambda : function.getIdleLambdas()) {
                        functionFootprint += LambdaMemoryUtils.getProcessMemory(function, lambda);
                    }
                }
                String username = Configuration.coder.decodeUsername(function.getName());
                String functionName = Configuration.coder.decodeFunctionName(function.getName());
                long timestamp = System.currentTimeMillis();
                responseBuilder.append(String.format(FOOTPRINT_METRIC, username, functionName, functionFootprint, timestamp));
                int allocatedLambdas = function.getRunningLambdas().size() + function.getIdleLambdas().size();
                responseBuilder.append(String.format(SCALABILITY_METRIC, username, functionName, allocatedLambdas, timestamp));
                responseBuilder.append(String.format(LATENCY_METRIC, username, functionName, buffer.max(), timestamp));
            }
            return Single.just(responseBuilder.toString());
        } catch (Throwable t) {
            return Single.just("");
        } finally {
            buffer.reset();
        }
    }

    public static void reportRequestTime(long time) {
        buffer.offer(time);
    }

}
