package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimerTask;
import java.util.logging.Level;


public class DefaultLambdaShutdownHandler extends TimerTask {

    private final Lambda lambda;
    private final Function function;
    private final ProcessBuilder process;

    public DefaultLambdaShutdownHandler(Lambda lambda, Function function) {
        this.lambda = lambda;
        this.function = function;
        this.process = lambda.getProcess();
    }

    public void printStream(Level level, InputStream stream) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(stream));
        String line;

        while ((line = is.readLine()) != null) {
            Logger.log(level, line);
        }
    }

    private void shutdownHotSpotLambda(String lambdaPath) throws Throwable {
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_hotspot.sh", lambdaPath).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", process.pid()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }

    private void shutdownVMMLambda(String lambdaPath) throws Throwable {
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_vmm.sh", lambdaPath).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", process.pid()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }

    private void shutdownLambda() {
        try {
            switch (lambda.getExecutionMode()) {
                case HOTSPOT:
                    shutdownHotSpotLambda(lambda.getLambdaPath());
                    break;
                case HOTSPOT_W_AGENT:
                    shutdownHotSpotLambda(lambda.getLambdaPath());
                    break;
                case NATIVE_IMAGE:
                    shutdownVMMLambda(lambda.getLambdaPath());
                    break;
                default:
                    Logger.log(Level.WARNING, String.format("Lambda ID=%d has no known execution mode: %s", process.pid(), lambda.getExecutionMode()));
            }
        } catch (Throwable t) {
            Logger.log(Level.SEVERE, String.format("Lambda ID=%d failed to shutdown: %s", process.pid(), t.getMessage()));
            t.printStackTrace();
        }
    }

    @Override
    public void run() {
        synchronized (function) {
            function.getIdleLambdas().remove(lambda);
        }

        lambda.getTimer().cancel();
        function.commissionLambda(lambda);
        lambda.resetClosedRequestCount();
        shutdownLambda();
        Configuration.argumentStorage.returnConnectionTriplet(lambda.getConnectionTriplet());

        synchronized (function) {
            Configuration.argumentStorage.deallocateMemoryLambda(function);
            function.notify();
        }
    }

}
