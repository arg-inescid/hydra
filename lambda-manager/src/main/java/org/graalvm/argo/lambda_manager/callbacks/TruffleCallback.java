package org.graalvm.argo.lambda_manager.callbacks;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.LambdaTruffleStatus;

public class TruffleCallback implements OnProcessFinishCallback {

    private final Lambda lambda;

    public TruffleCallback(Lambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish(int exitCode) {
        lambda.setTruffleStatus(LambdaTruffleStatus.NEED_REGISTRATION);
    }
}
