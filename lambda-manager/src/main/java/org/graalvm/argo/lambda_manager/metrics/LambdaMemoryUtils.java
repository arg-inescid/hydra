package org.graalvm.argo.lambda_manager.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LambdaMemoryUtils {

    private static final double KB_IN_MB = 1024;

    // Metrics collection for Firecracker VMs.
    public static Map<String, Double> collectMemoryMetricsFirecracker() {
        try {
            InputStream stream = executeCommand("bash", "-c", "ps -C firecracker -o rss=,args=");
            return readMemoryToMapFirecracker(stream);
        } catch (Throwable thr) {
            thr.printStackTrace();
            return new HashMap<>();
        }
    }

    private static double kilobytesToMegabytes(double sizeKb) {
        return sizeKb / KB_IN_MB;
    }

    private static Map<String, Double> readMemoryToMapFirecracker(InputStream stream) {
        Map<String, Double> rssRecords = new HashMap<>(128);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // The inner try/catch is needed to skip lambdas for which we cannot collect memory.
                try {
                    String rssString = line.substring(0, line.indexOf(' '));
                    double rss = rssString.isEmpty() ? 0 : kilobytesToMegabytes(Double.parseDouble(rssString));
                    // Each line has the the following format: "firecracker --socket /tmp/tapname.socket".
                    // Here, we retrieve "tapname" as a unique identifier of a Firecracker VM.
                    String lambdaId = line.substring(line.lastIndexOf("/tmp/") + 5, line.lastIndexOf('.'));
                    rssRecords.put(lambdaId, rss);
                } catch (Exception e) {
                    // e.printStackTrace();
                    // System.err.println(e.getMessage());
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return rssRecords;
    }

    private static InputStream executeCommand(String... command) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        process.waitFor();
        return process.getInputStream();
    }

    // Metrics collection for Docker containers.
    public static Map<String, Double> collectMemoryMetricsContainer() {
        try {
            InputStream stream = executeCommand("bash", "-c", "docker stats --all --no-stream --no-trunc --format \"{{.Name}} {{.MemUsage}}\"");
            return readMemoryToMapContainer(stream);
        } catch (Throwable thr) {
            thr.printStackTrace();
            return new HashMap<>();
        }
    }

    private static Map<String, Double> readMemoryToMapContainer(InputStream stream) {
        Map<String, Double> rssRecords = new HashMap<>(128);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // The inner try/catch is needed to skip lambdas for which we cannot collect memory.
                // Structure of the line: "lambda_228_GRAALVISOR 73.84MiB / 256MiB"
                try {
                    String lambdaName = line.substring(0, line.indexOf(' '));
                    String rssString = line.substring(line.indexOf(' ') + 1, line.indexOf("MiB"));
                    double rss = rssString.isEmpty() ? 0 : Double.parseDouble(rssString);
                    rssRecords.put(lambdaName, rss);
                } catch (Exception e) {
                    // e.printStackTrace();
                    // System.err.println(e.getMessage());
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return rssRecords;
    }

}
