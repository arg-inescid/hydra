package org.graalvm.argo.lambda_manager.utils;

import static org.graalvm.argo.lambda_manager.core.Environment.CODEBASE;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

public class LambdaMemoryUtils {

    private static final String RSS_REGEX = "(\\d+)";
    private static final String LAMBDA_PID_FILE = "lambda.pid";
    private static final String LAMBDA_ID_FILE = "lambda.id";

    private static final double KB_IN_MB = 1024;

    public static double getProcessMemory(Lambda lambda) throws IOException, InterruptedException {
        if (lambda.getExecutionMode() == LambdaExecutionMode.CUSTOM) {
            String lambdaId = getVmId(lambda);
            InputStream stream = executeCommand("ps", "aux");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            double sizeKb = parseOutputCr(reader, lambdaId);
            return kilobytesToMegabytes(sizeKb);
        } else {
            long pid = getLambdaPid(lambda);
            InputStream stream = executeCommand("ps", "eo", "rss", String.valueOf(pid));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            double sizeKb = parseOutput(readToString(reader));
            return kilobytesToMegabytes(sizeKb);
        }
    }

    private static double kilobytesToMegabytes(double sizeKb) {
        return sizeKb / KB_IN_MB;
    }

    private static double parseOutputCr(BufferedReader reader, String lambdaId) throws IOException {
        String line;
        try (reader) {
            while ((line = reader.readLine()) != null) {
                if (line.contains(lambdaId)) {
                    // retrieve the RSS column from the 'ps aux' output
                    return Double.parseDouble(line.split("\\s+")[5]);
                }
            }
        }
        return 0;
    }

    private static double parseOutput(String processOutput) {
        Pattern pattern = Pattern.compile(RSS_REGEX);
        Matcher matcher = pattern.matcher(processOutput);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0;
    }

    private static String getVmId(Lambda lambda) throws IOException, InterruptedException {
        File pidFile = Paths.get(CODEBASE, lambda.getLambdaName(), LAMBDA_ID_FILE).toFile();
        BufferedReader reader = new BufferedReader(new FileReader(pidFile));
        return readToString(reader);
    }

    private static long getLambdaPid(Lambda lambda) throws IOException, InterruptedException {
        File pidFile = Paths.get(CODEBASE, lambda.getLambdaName(), LAMBDA_PID_FILE).toFile();
        BufferedReader reader = new BufferedReader(new FileReader(pidFile));
        String parentPid = readToString(reader);

        InputStream stream = executeCommand("pgrep", "-P", parentPid);
        reader = new BufferedReader(new InputStreamReader(stream));
        return Long.parseLong(readToString(reader));
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

    private static InputStream executeCommand(String... command) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        process.waitFor();
        return process.getInputStream();
    }

}
