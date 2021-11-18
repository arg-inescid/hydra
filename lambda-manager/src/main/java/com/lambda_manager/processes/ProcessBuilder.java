package com.lambda_manager.processes;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import static com.lambda_manager.core.Environment.IS_ALIVE_PAUSE;

public class ProcessBuilder extends Thread {

    private final List<String> command;
    private final String commandAsString;
    private final OnProcessFinishCallback callback;
    private final String outputFilename;
    private final String processType;
    private final long pid;
    private Process process;

    public ProcessBuilder(String processType, long pid, List<String> command, OnProcessFinishCallback callback, String outputFilename) {
        this.command = command;
        this.commandAsString = Arrays.toString(command.toArray());
        this.callback = callback;
        this.outputFilename = outputFilename;
        this.processType = processType;
        this.pid = pid;
    }

    public long pid() {
        return this.pid;
    }

    private void logProcessStart() {
        if (Logger.isLoggable(Level.FINER)) {
            Logger.log(Level.FINER, String.format(Messages.PROCESS_START_FINE, pid, commandAsString, outputFilename));
        } else {
            Logger.log(Level.INFO, String.format(Messages.PROCESS_START_INFO, pid, processType, outputFilename));
        }
    }

    private void logProcessExit(int exitCode) {
        if (Logger.isLoggable(Level.FINER)) {
            Logger.log(Level.FINER, String.format(Messages.PROCESS_EXIT_FINE, pid, commandAsString, exitCode));
        } else {
            Logger.log(Level.INFO, String.format(Messages.PROCESS_EXIT_INFO, pid, processType, exitCode));
        }
    }

    private void logProcessException(Exception e) {
        if (Logger.isLoggable(Level.FINER)) {
            Logger.log(Level.WARNING, String.format(Messages.PROCESS_RAISE_EXCEPTION_FINE, pid, commandAsString), e);
        } else {
            Logger.log(Level.WARNING, String.format(Messages.PROCESS_RAISE_EXCEPTION_INFO, pid, processType), e);
        }
    }

    private void logProcessShutdownException(Throwable t) {
        if (Logger.isLoggable(Level.FINER)) {
            Logger.log(Level.SEVERE, String.format(Messages.PROCESS_SHUTDOWN_EXCEPTION_FINE, pid, commandAsString), t);
        } else {
            Logger.log(Level.SEVERE, String.format(Messages.PROCESS_SHUTDOWN_EXCEPTION_INFO, pid, commandAsString), t);
        }
    }

    @Override
    public void run() {
        try {
            java.lang.ProcessBuilder processBuilder = prepareStartup();
            this.process = processBuilder.start();
            int exitCode = process.waitFor();
            callback.finish(exitCode);
            logProcessExit(exitCode);
        } catch (IOException | InterruptedException e) {
            logProcessException(e);
        } catch (Throwable t) {
            logProcessShutdownException(t);
        }
    }

    private java.lang.ProcessBuilder prepareStartup() throws InterruptedException {
        File outputFile = new File(outputFilename);
        java.lang.ProcessBuilder processBuilder = new java.lang.ProcessBuilder();
        processBuilder.redirectOutput(outputFile).redirectError(outputFile);
        processBuilder.command(command);
        logProcessStart();
        return processBuilder;
    }

    public void shutdownInstance() {
        shutdownInstance(process.descendants());
    }

    private void shutdownInstance(Stream<ProcessHandle> descendants) {
        if (descendants == null) {
            return;
        }

        descendants.forEach(new Stream.Builder<>() {
            @Override
            public void accept(ProcessHandle processHandle) {
                shutdownInstance(processHandle.descendants());
                processHandle.destroy();
                while (processHandle.isAlive()) {
                    try {
                        Thread.sleep(IS_ALIVE_PAUSE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
