package com.lambda_manager.processes;

import com.lambda_manager.callbacks.OnProcessFinishCallback;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.lambda_manager.utils.Environment.IS_ALIVE_PAUSE;

public class ProcessBuilder extends Thread {

    private final List<String> command;
    private final OnProcessFinishCallback callback;
    private final String outputFilename;
    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final long pid;
    private Process process;

    public ProcessBuilder(List<String> command, OnProcessFinishCallback callback, String outputFilename, long pid) {
        this.command = command;
        this.callback = callback;
        this.outputFilename = outputFilename;
        this.pid = pid;
    }

    @Override
    public void run() {
        try {
            java.lang.ProcessBuilder processBuilder = prepareStartup();
            this.process = processBuilder.start();
            int exitCode = process.waitFor();
            callback.finish(exitCode);
            logger.log(Level.INFO, String.format("PID -> %d | Command -> %s | Exit code -> %d",
                    pid, Arrays.toString(command.toArray()), exitCode));
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, String.format("PID -> %d | Command -> %s | Raised exception! ",
                    pid, Arrays.toString(command.toArray())), e);
        }
    }

    private java.lang.ProcessBuilder prepareStartup() throws InterruptedException {
        File outputFile = new File(outputFilename);
        java.lang.ProcessBuilder processBuilder = new java.lang.ProcessBuilder();
        processBuilder.redirectOutput(outputFile).redirectError(outputFile);
        processBuilder.command(command);
        logger.log(Level.INFO, String.format("PID -> %d | Command -> %s | Output -> %s",
                pid, Arrays.toString(command.toArray()), outputFilename));

        return processBuilder;
    }

    public void shutdownInstance() {
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
                        //noinspection BusyWait
                        Thread.sleep(IS_ALIVE_PAUSE);
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
