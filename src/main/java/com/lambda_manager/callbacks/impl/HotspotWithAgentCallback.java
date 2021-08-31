package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.optimizers.FunctionStatus;

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
