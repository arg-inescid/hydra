package org.graalvm.argo.lambda_manager.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsScraper implements Runnable {

    private final BufferedWriter bw;
    private final ExecutorService service;

    public MetricsScraper(File output, ExecutorService executor) throws IOException {
        this.bw = new BufferedWriter(new FileWriter(output));
        this.service = executor;
        bw.write("[\n");
    }

    @Override
    public void run() {
        String record = MetricsProvider.getMetricsRecord();
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
