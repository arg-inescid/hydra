package com.lambda_manager.processes;

import com.lambda_manager.callbacks.OnProcessFinishCallback;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ProcessBuilder extends Thread {

    private final List<String> command;
    private final OnProcessFinishCallback callback;
    private final boolean destroyForcibly;
    private final String outputFilename;
    private Process process;
    private final Logger logger;

    public ProcessBuilder(List<String> command, boolean destroyForcibly, OnProcessFinishCallback callback,
                          String outputFilename) {
        this.command = command;
        this.destroyForcibly = destroyForcibly;
        this.callback = callback;
        this.outputFilename = outputFilename;
        this.logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    @Override
    public void run() {
        File outputFile = new File(outputFilename);
        command.add(String.valueOf(System.currentTimeMillis()));
        logger.log(Level.INFO, "Process -> " + Arrays.toString(command.toArray()) + ". Output/Error -> " + outputFilename);
        java.lang.ProcessBuilder processBuilder = new java.lang.ProcessBuilder();
        processBuilder.redirectOutput(outputFile).redirectError(outputFile);
        processBuilder.command(command);
        try {
            this.process = processBuilder.start();
            int code = process.waitFor();
            callback.finish();
            logger.log(Level.INFO, "Process -> " + Arrays.toString(command.toArray()) + ". Exit code -> " + code);
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Process -> " + Arrays.toString(command.toArray()) + " raise exception!", e);
        }
    }

    public void shutdownInstance() {
        shutdownInstance(process.descendants());
    }

    private void shutdownInstance(Stream<ProcessHandle> descendants) {
        if(descendants == null) {
            return;
        }

        descendants.forEach(new Stream.Builder<>() {
            @Override
            public void accept(ProcessHandle processHandle) {
                shutdownInstance(processHandle.descendants());
                if (destroyForcibly) {
                    processHandle.destroyForcibly();
                } else {
                    processHandle.destroy();
                }
            }

            @Override
            public Stream<ProcessHandle> build() {
                return null;
            }
        });
    }
}
