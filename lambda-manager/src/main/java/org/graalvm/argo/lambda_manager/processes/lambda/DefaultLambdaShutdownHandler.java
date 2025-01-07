package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import java.util.TimerTask;
import java.util.logging.Level;


public class DefaultLambdaShutdownHandler extends TimerTask {

    private final Lambda lambda;
    private final String reason;

    public DefaultLambdaShutdownHandler(Lambda lambda, String reason) {
        this.lambda = lambda;
        this.reason = reason;
    }

    @Override
    public void run() {
        Logger.log(Level.INFO, String.format("Terminating lambda %d for reason: %s.", lambda.getLambdaID(), reason));
        // Remove lambda from global state.
        LambdaManager.lambdas.remove(lambda);

        // Reset the auto-shutdown timer.
        if (lambda.getTimer() != null) {
            lambda.getTimer().cancel();
        }

        // Reset request counts.
        lambda.resetClosedRequestCount();

        // Shutdown lambda and replenish the lambda pool.
        try {
            Configuration.argumentStorage.getLambdaPool().disposeLambda(lambda);
        } catch (InterruptedException interruptedException) {
            Logger.log(Level.WARNING, Messages.ERROR_TAP_REMOVAL, interruptedException);
        }
    }

}
