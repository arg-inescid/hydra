package com.serverless_demo.callbacks.impl;

import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.optimizers.LambdaStatusType;
import com.serverless_demo.utils.Tuple;

public class AgentConfigReadyCallback implements OnProcessFinishCallback {

    private final Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;

    public AgentConfigReadyCallback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish() {
        lambda.list.setStatus(LambdaStatusType.NOT_BUILT_CONFIGURED);
    }
}
