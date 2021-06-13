package com.lambda_manager.handlers;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.utils.LambdaTuple;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultLambdaShutdownHandler extends TimerTask {

    private final LambdaTuple<Function, Lambda> lambda;
    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public DefaultLambdaShutdownHandler(LambdaTuple<Function, Lambda> lambda) {
        this.lambda = lambda;
    }

    private void shutdownHotSpotLambda(String lambdaPath) throws Throwable {
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_hotspot.sh", lambdaPath).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.lambda.pid()));
            Processes.printProcessErrorStream(logger, Level.WARNING, p);
        }
    }

    private void shutdownLambda() {
        try {

            switch (lambda.lambda.getExecutionMode()) {
                case HOTSPOT:
                    shutdownHotSpotLambda(String.format("codebase/%s/pid_%d_hotspot", lambda.function.getName(), lambda.lambda.pid()));
                    break;
                case HOTSPOT_W_AGENT:
                    shutdownHotSpotLambda(String.format("codebase/%s/pid_%d_hotspot_w_agent", lambda.function.getName(), lambda.lambda.pid()));
                    break;
                case NATIVE_IMAGE:
                    // Currently we don't shutdown lambdas running in Native Image.
                    break;
                default:
                    logger.log(Level.WARNING, String.format("Lambda ID=%d has no known execution mode: %s", lambda.lambda.pid(), lambda.lambda.getExecutionMode()));
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, String.format("Lambda ID=%d failed to shutdown: %s", lambda.lambda.pid(), t.getMessage()));
            t.printStackTrace();
        }
    }

    @Override
    public void run() {
        ProcessBuilder processBuilder;

        synchronized (lambda.function) {
            if (!lambda.function.getIdleLambdas().remove(lambda.lambda)) {
                return;
            }
            lambda.lambda.getTimer().cancel();
            processBuilder = lambda.function.removeProcess(lambda.lambda.pid());
        }

        lambda.function.commissionLambda(lambda.lambda);
        lambda.lambda.resetClosedRequestCount();

        shutdownLambda();
        processBuilder.shutdownInstance();

        synchronized (lambda.function) {
            Configuration.argumentStorage.returnConnectionTriplet(lambda.lambda.getConnectionTriplet());
            lambda.function.getStoppedLambdas().add(lambda.lambda);
            lambda.function.notify();
        }
    }

}
