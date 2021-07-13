package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.optimizers.FunctionStatus;

public class VMMCallback implements OnProcessFinishCallback {

    private final Lambda lambda;

    public VMMCallback(Lambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish(int exitCode) {
        if (exitCode != 0) {
            // Need fallback to execution with Hotspot with agent.
            lambda.getTimer().cancel();
            if (lambda.getFunction().getStatus() == FunctionStatus.BUILT) {
                lambda.getFunction().setStatus(FunctionStatus.NOT_BUILT_NOT_CONFIGURED);
            }
        }
    }
}
