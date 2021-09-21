package com.lambda_manager.utils;

import static com.lambda_manager.core.Environment.CODEBASE;
import static com.lambda_manager.core.Environment.HOTSPOT;
import static com.lambda_manager.core.Environment.HOTSPOT_W_AGENT;
import static com.lambda_manager.core.Environment.VMM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;

public class LambdaMemoryUtils {

    private static final String RSS_REGEX = "(\\d+)";
    private static final String LAMBDA_PID_FILE = "lambda.pid";

    private static final double KB_IN_MB = 1024;

    public static double getProcessMemory(Function function, Lambda lambda) throws IOException, InterruptedException {
        long pid = getLambdaPid(function, lambda);
        InputStream stream = executeCommand("ps", "eo", "rss", String.valueOf(pid));
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        double sizeKb = parseOutput(readToString(reader));
        return kilobytesToMegabytes(sizeKb);
    }

    private static double kilobytesToMegabytes(double sizeKb) {
        return sizeKb / KB_IN_MB;
    }

    private static double parseOutput(String processOutput) {
        Pattern pattern = Pattern.compile(RSS_REGEX);
        Matcher matcher = pattern.matcher(processOutput);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0;
    }

    private static long getLambdaPid(Function function, Lambda lambda) throws IOException, InterruptedException {
        String lambdaMode = null;
        switch (lambda.getExecutionMode()) {
            case HOTSPOT_W_AGENT:
                lambdaMode = HOTSPOT_W_AGENT;
                break;
            case HOTSPOT:
                lambdaMode = HOTSPOT;
                break;
            case NATIVE_IMAGE:
                lambdaMode = VMM;
                break;
        }
        File pidFile = Paths.get(CODEBASE, function.getName(), String.format(lambdaMode, lambda.getProcess().pid()), LAMBDA_PID_FILE).toFile();
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
        Process pgrepProcess = pb.start();
        pgrepProcess.waitFor();
        return pgrepProcess.getInputStream();
    }

}
