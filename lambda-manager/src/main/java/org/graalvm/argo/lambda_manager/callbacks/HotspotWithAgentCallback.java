package org.graalvm.argo.lambda_manager.callbacks;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;

public class HotspotWithAgentCallback implements OnProcessFinishCallback {

    private final Lambda lambda;
    private final Function function;
    private final OnProcessFinishCallback callback;

    public HotspotWithAgentCallback(Lambda lambda, Function function, OnProcessFinishCallback callback) {
        this.lambda = lambda;
        this.function = function;
        this.callback = callback;
    }

    @Override
    public void finish(int exitCode) {
        function.setLastAgentPID(lambda.getProcess().pid());
        function.setStatus(FunctionStatus.NOT_BUILT_CONFIGURED);
        callback.finish(exitCode);
    }
}
