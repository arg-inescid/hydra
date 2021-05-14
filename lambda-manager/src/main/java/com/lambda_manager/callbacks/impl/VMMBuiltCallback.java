package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.utils.LambdaTuple;

public class VMMBuiltCallback implements OnProcessFinishCallback {

    private final LambdaTuple<Function, Lambda> lambda;

    public VMMBuiltCallback(LambdaTuple<Function, Lambda> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish(int exitCode) {
        lambda.function.setStatus(FunctionStatus.BUILT);
        lambda.function.setUpdateID(true);
    }
}
