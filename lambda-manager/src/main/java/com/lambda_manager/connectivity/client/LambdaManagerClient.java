package com.lambda_manager.connectivity.client;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.utils.Tuple;

public interface LambdaManagerClient {
    void createNewClient(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration)
            throws ErrorUploadingNewLambda;
    String sendRequest(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);
}