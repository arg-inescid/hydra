package org.graalvm.argo.lambda_manager.utils;

import static org.graalvm.argo.lambda_manager.core.Environment.CODEBASE;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class LambdaMemoryUtils {

    private static final String LAMBDA_PID_FILE = "lambda.pid";

    private static final double KB_IN_MB = 1024;

    public static Map<String, Double> collectMemoryMetrics() {
        try {
            InputStream stream = executeCommand("bash", "-c", "ps -C firecracker -o rss=,args=");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            return readMemoryToMap(reader);
        } catch (Throwable thr) {
            thr.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * This method is specific to collecting memory metrics only for NIUk lambdas.
     */
    public static double getProcessMemory(Lambda lambda) {
        try {
            String pid = getLambdaPid(lambda);
            InputStream stream = executeCommand("ps", "eo", "rss", pid);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            double sizeKb = parseOutput(readToString(reader));
            return kilobytesToMegabytes(sizeKb);
        } catch (Throwable thr) {
            thr.printStackTrace();
            return 0;
        }
    }

    private static double kilobytesToMegabytes(double sizeKb) {
        return sizeKb / KB_IN_MB;
    }

    private static double parseOutput(String processOutput) {
        processOutput = processOutput.replaceAll("[^-\\d.]", "");
        return processOutput.isEmpty() ? 0 : Double.parseDouble(processOutput);
    }

    private static String getLambdaPid(Lambda lambda) throws IOException, InterruptedException {
        File pidFile = Paths.get(CODEBASE, lambda.getLambdaName(), LAMBDA_PID_FILE).toFile();
        BufferedReader reader = new BufferedReader(new FileReader(pidFile));
        String parentPid = readToString(reader);

        InputStream stream = executeCommand("pgrep", "-P", parentPid);
        reader = new BufferedReader(new InputStreamReader(stream));
        return readToString(reader);
    }

    private static String readToString(BufferedReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        String line;
        try (reader) {
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static Map<String, Double> readMemoryToMap(BufferedReader reader) throws IOException {
        Map<String, Double> rssRecords = new HashMap<>(64);
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                String rssString = line.substring(0, line.indexOf(' '));
                double rss = rssString.isEmpty() ? 0 : kilobytesToMegabytes(Double.parseDouble(rssString));
                String lambdaId = line.substring(line.lastIndexOf(' ') + 1);
                rssRecords.put(lambdaId, rss);
            }
        }
        return rssRecords;
    }

    private static InputStream executeCommand(String... command) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        process.waitFor();
        return process.getInputStream();
    }

}
