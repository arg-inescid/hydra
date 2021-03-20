package com.serverless_demo.callbacks.impl;

import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.exceptions.user.ErrorUploadingNewLambda;
import com.serverless_demo.optimizers.LambdaStatusType;
import com.serverless_demo.utils.Tuple;

public class CheckClientStatus implements OnProcessFinishCallback {

    private final Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;
    private final LambdaManagerConfiguration configuration;

    public CheckClientStatus(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        this.lambda = lambda;
        this.configuration = configuration;
    }

    @Override
    public void finish() {
        try {
            if(lambda.list.getStatus() == LambdaStatusType.BUILT) {
                configuration.client.createNewClient(lambda, configuration,false);
            }
        } catch (ErrorUploadingNewLambda errorUploadingNewLambda) {
            errorUploadingNewLambda.printStackTrace();
        }
    }
}
