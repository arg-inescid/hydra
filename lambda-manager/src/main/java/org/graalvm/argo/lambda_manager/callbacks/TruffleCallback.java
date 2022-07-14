package org.graalvm.argo.lambda_manager.callbacks;

import org.graalvm.argo.lambda_manager.core.Lambda;

// TODO - change the name. We need to do this for all Graalvisor and CustomRT
public class TruffleCallback implements OnProcessFinishCallback {

    private final Lambda lambda;

    public TruffleCallback(Lambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish(int exitCode) {
        lambda.resetRegisteredInLambda();
    }
}
