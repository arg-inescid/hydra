package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.utils.LambdaTuple;

public class AgentConfigReadyCallback implements OnProcessFinishCallback {

    private final LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;

    public AgentConfigReadyCallback(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish() {
        lambda.list.setStatus(LambdaStatusType.NOT_BUILT_CONFIGURED);
    }
}
