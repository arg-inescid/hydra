package org.graalvm.argo.lambda_manager.callbacks;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;

public class VMMCallback implements OnProcessFinishCallback {

    private final Lambda lambda;
    private final Function function;

    public VMMCallback(Lambda lambda, Function function) {
        this.lambda = lambda;
        this.function = function;
    }

    @Override
    public void finish(int exitCode) {
        if (exitCode != 0) {
            // Need fallback to execution with Hotspot with agent.
            lambda.getTimer().cancel();
            if (function.getStatus() == FunctionStatus.READY) {
                function.setStatus(FunctionStatus.NOT_BUILT_NOT_CONFIGURED);
            }
        }
    }
}
