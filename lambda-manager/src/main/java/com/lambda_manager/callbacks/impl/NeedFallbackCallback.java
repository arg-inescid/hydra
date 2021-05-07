package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.utils.LambdaTuple;

public class NeedFallbackCallback implements OnProcessFinishCallback {

    private final LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;

    public NeedFallbackCallback(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish(int exitCode) {
        if(exitCode != 0) {
            // Need fallback to execution with Hotspot with agent.
            lambda.instance.getTimer().cancel();
            if(lambda.list.getStatus() == LambdaStatusType.BUILT) {
                lambda.list.setStatus(LambdaStatusType.NOT_BUILT_NOT_CONFIGURED);
            }
        }
    }
}
