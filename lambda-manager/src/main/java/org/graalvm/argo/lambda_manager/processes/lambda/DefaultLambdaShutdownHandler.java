package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.core.LambdaType;
import org.graalvm.argo.lambda_manager.schedulers.RoundedRobinScheduler;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimerTask;
import java.util.logging.Level;


public class DefaultLambdaShutdownHandler extends TimerTask {

    private final Lambda lambda;

    public DefaultLambdaShutdownHandler(Lambda lambda) {
        this.lambda = lambda;
    }

    public void printStream(Level level, InputStream stream) throws IOException {
        BufferedReader is = new BufferedReader(new InputStreamReader(stream));
        String line;

        while ((line = is.readLine()) != null) {
            Logger.log(level, line);
        }
    }

    private void shutdownFirecrackerContainerdLambda(String lambdaPath) throws Throwable {
        String lambdaMode = lambda.getExecutionMode().toString();
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_cruntime.sh", lambdaPath, lambdaMode,
                lambda.getConnection().ip, String.valueOf(lambda.getConnection().port)).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }

    private void shutdownFirecrackerLambda(String lambdaPath, LambdaType lambdaType) throws Throwable {
        String lambdaMode = lambda.getExecutionMode().toString();
        // Append lambda ID to command only if lambda was restored from snapshot (to terminate it properly).
        String lambdaId = lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT ? String.valueOf(lambda.getLambdaID()) : "";
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_firecracker.sh", lambdaPath, lambdaMode,
                lambda.getConnection().ip, String.valueOf(lambda.getConnection().port), lambda.getConnection().tap, lambdaId).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }

    private void shutdownContainerLambda(String lambdaPath) throws Throwable {
        String lambdaMode = lambda.getExecutionMode().toString();
        Process p = new java.lang.ProcessBuilder("bash", "src/scripts/stop_container.sh", lambdaPath, lambdaMode,
                lambda.getConnection().ip, String.valueOf(lambda.getConnection().port), lambda.getLambdaName()).start();
        p.waitFor();
        if (p.exitValue() != 0) {
            Logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfully", lambda.getLambdaID()));
            printStream(Level.WARNING, p.getErrorStream());
        }
    }

    private void shutdownLambda() {
        try {
            LambdaType lambdaType = Configuration.argumentStorage.getLambdaType();
            if (lambdaType == LambdaType.CONTAINER) {
                shutdownContainerLambda(Environment.CODEBASE + "/" + lambda.getLambdaName());
            } else if (lambdaType == LambdaType.VM_FIRECRACKER || lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT) {
                shutdownFirecrackerLambda(Environment.CODEBASE + "/" + lambda.getLambdaName(), lambdaType);
            } else if (lambdaType == LambdaType.VM_CONTAINERD) {
                shutdownFirecrackerContainerdLambda(Environment.CODEBASE + "/" + lambda.getLambdaName());
            } else {
                Logger.log(Level.WARNING, String.format("Lambda ID=%d has no known execution mode: %s", lambda.getLambdaID(), lambda.getExecutionMode()));
            }
        } catch (Throwable t) {
            Logger.log(Level.SEVERE, String.format("Lambda ID=%d failed to shutdown: %s", lambda.getLambdaID(), t.getMessage()));
            t.printStackTrace();
        }
    }

    @Override
    public void run() {
        Logger.log(Level.INFO, String.format("Terminating lambda %d.", lambda.getLambdaID()));
        // Remove lambda form global state.
        LambdaManager.lambdas.remove(lambda);

        // Reset the auto-shutdown timer.
        if (lambda.getTimer() != null) {
            lambda.getTimer().cancel();
        }

        // Shutdown lambda.
        shutdownLambda();

        // TODO - we need to rething this...
        if (lambda.isDecommissioned()) {
            RoundedRobinScheduler.hasDecommissionedLambdas = false;
        }

        // Reset request counts.
        lambda.resetClosedRequestCount();

        // Return connection to connection pool.
        Configuration.argumentStorage.returnLambdaConnection(lambda.getConnection());

        // Return all memory to the memory pool.
        Configuration.argumentStorage.getMemoryPool().deallocateMemoryLambda(lambda.getMemoryPool().getMaxMemory());
        lambda.getMemoryPool().reset();
    }

}
