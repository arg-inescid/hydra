package com.serverless_demo.connectivity.client;

import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.exceptions.user.ErrorUploadingNewLambda;
import com.serverless_demo.utils.Tuple;

public interface LambdaManagerClient {

    void createNewClient(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration,
                                       boolean createLocalAddress) throws ErrorUploadingNewLambda;
    String sendRequest(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);
}