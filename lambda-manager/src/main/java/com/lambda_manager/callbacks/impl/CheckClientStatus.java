package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.utils.Tuple;

public class CheckClientStatus implements OnProcessFinishCallback {

    private final Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;
    private final LambdaManagerConfiguration configuration;

    public CheckClientStatus(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        this.lambda = lambda;
        this.configuration = configuration;
    }

    @Override
    public void finish() {
//        try {
//            if(lambda.list.getStatus() == LambdaStatusType.BUILT) {
//                configuration.client.createNewClient(lambda, configuration,false);
//            }
//        } catch (ErrorUploadingNewLambda errorUploadingNewLambda) {
//            errorUploadingNewLambda.printStackTrace();
//        }
    }
}
