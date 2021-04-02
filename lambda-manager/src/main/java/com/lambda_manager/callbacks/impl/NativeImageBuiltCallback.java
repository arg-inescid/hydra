package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.utils.Tuple;

public class NativeImageBuiltCallback implements OnProcessFinishCallback {

    private final Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;

    public NativeImageBuiltCallback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
        this.lambda = lambda;
    }

    @Override
    public void finish() {
        lambda.list.setStatus(LambdaStatusType.BUILT);
    }
}
