package com.lambda_manager.processes;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.utils.logger.ElapseTimer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ProcessBuilder extends Thread {

    private final List<String> command;
    private final OnProcessFinishCallback callback;
    private final String outputFilename;
    private Process process;
    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private long timestamp;

    public ProcessBuilder(List<String> command, OnProcessFinishCallback callback, String outputFilename) {
        this.command = command;
        this.callback = callback;
        this.outputFilename = outputFilename;
    }

    @Override
    public void run() {
        try {
            java.lang.ProcessBuilder processBuilder = prepareStartup();
            this.process = processBuilder.start();
            int code = process.waitFor();
            callback.finish();
            logger.log(Level.INFO, "Process -> " + Arrays.toString(command.toArray()) + ". Exit code -> " + code);
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Process -> " + Arrays.toString(command.toArray()) + " raise exception!", e);
        }
    }

    private java.lang.ProcessBuilder prepareStartup() throws InterruptedException {
        this.timestamp = ElapseTimer.elapsedTime();
        File outputFile = new File(outputFilename);

        java.lang.ProcessBuilder processBuilder = new java.lang.ProcessBuilder();
        processBuilder.redirectOutput(outputFile).redirectError(outputFile);
        processBuilder.command(command);
        logger.log(Level.INFO, timestamp + "#Process -> "
                + Arrays.toString(command.toArray()) + ". Output/Error -> " + outputFilename);

        return processBuilder;
    }

    private void writeTimestamp(long timestamp) {
        File outputFile = new File(outputFilename);
        try (FileWriter fileWriter = new FileWriter(outputFile, true)) {
            fileWriter.write("\n***** USAGE INFO *****\n");
            fileWriter.write("Timestamp (" + timestamp + ")\n"); // TODO - this needs a more descriptive header.
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void shutdownInstance() {
        writeTimestamp(timestamp);
        // TODO - double check that we have descendants.
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
                processHandle.destroy();
                while (processHandle.isAlive()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace(); // TODO - properly handle this exception
                    }
                }
            }

            @Override
            public Stream<ProcessHandle> build() {
                return null;
            }
        });
    }
}
