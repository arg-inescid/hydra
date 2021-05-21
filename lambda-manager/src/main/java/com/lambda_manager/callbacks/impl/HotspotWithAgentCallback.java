package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.utils.LambdaTuple;

public class HotspotWithAgentCallback implements OnProcessFinishCallback {

    private final LambdaTuple<Function, Lambda> lambda;
    private final OnProcessFinishCallback callback;

    public HotspotWithAgentCallback(LambdaTuple<Function, Lambda> lambda,
                                    OnProcessFinishCallback callback) {
        this.lambda = lambda;
        this.callback = callback;
    }

    @Override
    public void finish(int exitCode) {
        lambda.function.setStatus(FunctionStatus.NOT_BUILT_CONFIGURED);
        callback.finish(exitCode);
    }
}
