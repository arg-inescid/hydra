package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.utils.LambdaTuple;

public class VMMCallback implements OnProcessFinishCallback {

    private final LambdaTuple<Function, Lambda> lambda;

    public VMMCallback(LambdaTuple<Function, Lambda> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish(int exitCode) {
        if (exitCode != 0) {
            // Need fallback to execution with Hotspot with agent.
            lambda.lambda.getTimer().cancel();
            if (lambda.function.getStatus() == FunctionStatus.BUILT) {
                lambda.function.setStatus(FunctionStatus.NOT_BUILT_NOT_CONFIGURED);
            }
        }
    }
}
