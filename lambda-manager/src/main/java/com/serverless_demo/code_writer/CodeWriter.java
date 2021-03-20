package com.serverless_demo.code_writer;

import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.exceptions.user.ErrorUploadingNewLambda;
import com.serverless_demo.utils.Tuple;

import java.io.IOException;

public interface CodeWriter {
    Tuple<LambdaInstancesInfo, LambdaInstanceInfo> upload(
            LambdaInstancesInfo lambdaInstancesInfo, String encodedName,
            byte[] lambdaCode) throws ErrorUploadingNewLambda, IOException;
    void remove(String encodedName);
}
