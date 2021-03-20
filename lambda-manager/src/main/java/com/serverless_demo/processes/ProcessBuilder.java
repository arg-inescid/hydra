package com.serverless_demo.processes;

import com.serverless_demo.callbacks.OnProcessFinishCallback;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class ProcessBuilder extends Thread {

    private final List<String> command;
    private final OnProcessFinishCallback callback;
    private final boolean destroyForcibly;
    private Process process;

    public ProcessBuilder(List<String> command, boolean destroyForcibly, OnProcessFinishCallback callback) {
        this.command = command;
        this.destroyForcibly = destroyForcibly;
        this.callback = callback;
    }

    @Override
    public void run() {
        java.lang.ProcessBuilder processBuilder = new java.lang.ProcessBuilder();
        processBuilder.redirectOutput(java.lang.ProcessBuilder.Redirect.INHERIT)
                .redirectError(java.lang.ProcessBuilder.Redirect.INHERIT);
        processBuilder.command(command);
        try {
            this.process = processBuilder.start();
            process.waitFor();
            callback.finish();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }

    public void shutdownInstance() {
        Stream<ProcessHandle> descendants = process.descendants();
        descendants.forEach(new Stream.Builder<>() {
            @Override
            public void accept(ProcessHandle processHandle) {
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
        if (destroyForcibly) {
            process.destroyForcibly();
        } else {
            process.destroy();
        }
    }
}
