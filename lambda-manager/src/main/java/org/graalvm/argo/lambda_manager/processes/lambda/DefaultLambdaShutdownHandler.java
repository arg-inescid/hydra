package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.schedulers.RoundedRobinScheduler;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import java.util.TimerTask;
import java.util.logging.Level;


public class DefaultLambdaShutdownHandler extends TimerTask {

    private final Lambda lambda;

    public DefaultLambdaShutdownHandler(Lambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public void run() {
        Logger.log(Level.INFO, String.format("Terminating lambda %d.", lambda.getLambdaID()));
        // Remove lambda from global state.
        LambdaManager.lambdas.remove(lambda);

        // Reset the auto-shutdown timer.
        if (lambda.getTimer() != null) {
            lambda.getTimer().cancel();
        }

        // TODO - we need to rethink this...
        if (lambda.isDecommissioned()) {
            RoundedRobinScheduler.hasDecommissionedLambdas = false;
        }

        // Reset request counts.
        lambda.resetClosedRequestCount();

        // Shutdown lambda and replenish the lambda pool.
        try {
            Configuration.argumentStorage.getLambdaPool().disposeLambda(lambda);
        } catch (InterruptedException interruptedException) {
            Logger.log(Level.WARNING, Messages.ERROR_TAP_REMOVAL, interruptedException);
        }

        lambda.getMemoryPool().reset();
    }

}
