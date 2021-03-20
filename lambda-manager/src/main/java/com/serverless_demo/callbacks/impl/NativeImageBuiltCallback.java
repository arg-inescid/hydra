package com.serverless_demo.callbacks.impl;

import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.exceptions.user.ErrorUploadingNewLambda;
import com.serverless_demo.optimizers.LambdaStatusType;
import com.serverless_demo.utils.Tuple;

public class NativeImageBuiltCallback implements OnProcessFinishCallback {

    private final Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;
    private final LambdaManagerConfiguration configuration;

    public NativeImageBuiltCallback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        this.lambda = lambda;
        this.configuration = configuration;
    }

    @Override
    public void finish() {
        try {
            configuration.client.createNewClient(lambda, configuration,false);
            lambda.list.setStatus(LambdaStatusType.BUILT);
        } catch (ErrorUploadingNewLambda errorUploadingNewLambda) {
            errorUploadingNewLambda.printStackTrace();
        }
    }
}
