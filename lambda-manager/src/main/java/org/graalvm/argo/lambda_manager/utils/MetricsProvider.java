package org.graalvm.argo.lambda_manager.utils;

import java.util.Map;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;

import io.reactivex.Single;

/**
 * MetricsProvider is specific to the Prometheus metrics format
 */
public class MetricsProvider {

    private static Buffer buffer = Buffer.create();

    private static final String FOOTPRINT_METRIC = "lambda_footprint{name=\"%s\"} %.3f %d\n";
    private static final String LATENCY_METRIC = "request_latency{user=\"%s\",function=\"%s\"} %d %d\n";


    public static Single<String> getFootprintAndScalability() {
        try {
            long timestamp = System.currentTimeMillis();
            Map<String, Function> functionsMap = Configuration.storage.getAll();
            StringBuilder responseBuilder = new StringBuilder();

            for (Lambda lambda : LambdaManager.lambdas) {
                double lambdaMemory = LambdaMemoryUtils.getProcessMemory(lambda);
                responseBuilder.append(String.format(FOOTPRINT_METRIC, lambda.getLambdaName(), lambdaMemory, timestamp));
            }

            for (Map.Entry<String, Function> entry : functionsMap.entrySet()) {
                Function function = entry.getValue();
                String username = Configuration.coder.decodeUsername(function.getName());
                String functionName = Configuration.coder.decodeFunctionName(function.getName());
                responseBuilder.append(String.format(LATENCY_METRIC, username, functionName, buffer.max(), timestamp));
            }
            return Single.just(responseBuilder.toString());
        } catch (Throwable t) {
            t.printStackTrace();
            return Single.just("");
        } finally {
            buffer.reset();
        }
    }

    public static void reportRequestTime(long time) {
        buffer.offer(time);
    }

}
