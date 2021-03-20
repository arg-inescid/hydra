package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.utils.Tuple;

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
