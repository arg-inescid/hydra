package org.graalvm.argo.lambda_manager.utils;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;

import io.reactivex.Single;

/**
 * MetricsProvider is specific to the Prometheus metrics format
 */
public class MetricsProvider {

    private static Buffer buffer = Buffer.create();

    private static final String LAMBDA_FOOTPRINT = "lambda_footprint{name=\"%s\"} %.3f %d\n";
    private static final String SYSTEM_FOOTPRINT = "system_footprint %.3f %d\n";
    private static final String REQUEST_LATENCY_MAX = "request_latency_max %d %d\n";
    private static final String REQUEST_LATENCY_AVG = "request_latency_avg %.3f %d\n";


    public static Single<String> getFootprintAndScalability() {
        try {
            long timestamp = System.currentTimeMillis();
            StringBuilder responseBuilder = new StringBuilder();

            double totalMemory = 0;
            for (Lambda lambda : LambdaManager.lambdas) {
                double lambdaMemory = LambdaMemoryUtils.getProcessMemory(lambda);
                totalMemory += lambdaMemory;
                responseBuilder.append(String.format(LAMBDA_FOOTPRINT, lambda.getLambdaName(), lambdaMemory, timestamp));
            }

            responseBuilder.append(String.format(SYSTEM_FOOTPRINT, totalMemory, timestamp));
            responseBuilder.append(String.format(REQUEST_LATENCY_MAX, buffer.max(), timestamp));
            responseBuilder.append(String.format(REQUEST_LATENCY_AVG, buffer.avg(), timestamp));
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
