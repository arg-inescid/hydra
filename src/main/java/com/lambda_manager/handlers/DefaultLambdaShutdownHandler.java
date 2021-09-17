package com.lambda_manager.handlers;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.processes.ProcessBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultLambdaShutdownHandler extends TimerTask {

    private final Lambda lambda;
    private final Function function;
    private final ProcessBuilder process;
    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public DefaultLambdaShutdownHandler(Lambda lambda) {
        this.lambda = lambda;
        this.function = lambda.getFunction();
        this.process = lambda.getProcess();
    }

    public void printStream(Level level, InputStream stream) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(stream));
        String line;

        while ((line = is.readLine()) != null) {
            logger.log(level, line);
        }
    }

    private void shutdownHotSpotLambda(String lambdaPath) throws Throwable {
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_hotspot.sh", lambdaPath).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", process.pid()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }

    private void shutdownLambda() {
        try {

            switch (lambda.getExecutionMode()) {
                case HOTSPOT:
                    shutdownHotSpotLambda(String.format("codebase/%s/pid_%d_hotspot", function.getName(), process.pid()));
                    break;
                case HOTSPOT_W_AGENT:
                    shutdownHotSpotLambda(String.format("codebase/%s/pid_%d_hotspot_w_agent", function.getName(), process.pid()));
                    break;
                case NATIVE_IMAGE:
                    // Currently, we don't shut down lambdas running in Native Image.
                    break;
                default:
                    logger.log(Level.WARNING, String.format("Lambda ID=%d has no known execution mode: %s", process.pid(), lambda.getExecutionMode()));
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, String.format("Lambda ID=%d failed to shutdown: %s", process.pid(), t.getMessage()));
            t.printStackTrace();
        }
    }

    @Override
    public void run() {

        synchronized (function) {
            if (!function.getIdleLambdas().remove(lambda)) {
                return;
            }
            lambda.getTimer().cancel();
        }

        function.commissionLambda(lambda);
        lambda.resetClosedRequestCount();

        shutdownLambda();
        process.shutdownInstance();

        synchronized (function) {
            Configuration.argumentStorage.returnConnectionTriplet(lambda.getConnectionTriplet());
            function.getStoppedLambdas().add(lambda);
            function.notify();
        }
    }

}
