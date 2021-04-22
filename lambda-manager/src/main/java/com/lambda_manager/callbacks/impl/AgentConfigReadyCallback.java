package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.utils.LambdaTuple;

public class AgentConfigReadyCallback implements OnProcessFinishCallback {

    private final LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;
    private final OnProcessFinishCallback callback;

    public AgentConfigReadyCallback(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                                    OnProcessFinishCallback callback) {
        this.lambda = lambda;
        this.callback = callback;
    }

    @Override
    public void finish() {
        lambda.list.setStatus(LambdaStatusType.NOT_BUILT_CONFIGURED);
        callback.finish();
    }
}
