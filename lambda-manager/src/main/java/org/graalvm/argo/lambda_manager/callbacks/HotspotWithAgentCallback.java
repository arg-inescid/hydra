package org.graalvm.argo.lambda_manager.callbacks;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;

public class HotspotWithAgentCallback implements OnProcessFinishCallback {

    private final Lambda lambda;
    private final OnProcessFinishCallback callback;

    public HotspotWithAgentCallback(Lambda lambda, OnProcessFinishCallback callback) {
        this.lambda = lambda;
        this.callback = callback;
    }

    @Override
    public void finish(int exitCode) {
        lambda.getFunction().setLastAgentPID(lambda.getProcess().pid());
        lambda.getFunction().setStatus(FunctionStatus.NOT_BUILT_CONFIGURED);
        callback.finish(exitCode);
    }
}
