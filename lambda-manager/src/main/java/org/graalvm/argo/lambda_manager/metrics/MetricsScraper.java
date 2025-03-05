package org.graalvm.argo.lambda_manager.metrics;

import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerPool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsScraper implements Runnable {

    private final BufferedWriter bw;
    private final ExecutorService service;
    private boolean isFirstRecord = true;
    private final String lambdaFootprintCommand;

    public MetricsScraper(File output, ExecutorService executor, LambdaManagerPool lambdaPool) throws IOException {
        this.bw = new BufferedWriter(new FileWriter(output));
        this.service = executor;
        if (lambdaPool.getGraalvisor() > 0) {
            lambdaFootprintCommand = "sudo smem -P \"polyglot-proxy\" -a -c \"name pss\" | grep \"polyglot-proxy\" | awk '{print $NF}'";
        } else if (lambdaPool.getCustomJava() > 0) {
            lambdaFootprintCommand = "sudo smem -P \"javaAction-all\" -a -c \"name pss\" | grep \"java\" | awk '{print $NF}'";
        } else if (lambdaPool.getGraalOS() > 0) {
            lambdaFootprintCommand = "sudo smem -P \"graalhost/graalhost\" -a -c \"name pss\" | grep \"GraalHub\" | awk '{print $NF}'";
        } else {
            lambdaFootprintCommand = null;
        }
        bw.write("[\n");
    }

    @Override
    public void run() {
        String record = MetricsProvider.getMetricsRecord(lambdaFootprintCommand);
        if (isFirstRecord) {
            isFirstRecord = false;
        } else {
            record = ",\n".concat(record);
        }
        try {
            bw.write(record);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        // First gracefully shutdown the service to ensure that we do not close
        // BufferedWriter during task execution.
        service.shutdown();
        while (true) {
            try {
                if (service.awaitTermination(10, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
            }
        }
        // Then close the BufferedWriter to flush all bytes to the file.
        try {
            bw.write("\n]");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
