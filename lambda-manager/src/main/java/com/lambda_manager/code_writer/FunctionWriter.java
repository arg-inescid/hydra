package com.lambda_manager.code_writer;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.utils.LambdaTuple;

import java.io.IOException;

public interface FunctionWriter {
    LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> upload(LambdaInstancesInfo lambdaInstancesInfo, String encodedName,
                                                                byte[] lambdaCode) throws ErrorUploadingNewLambda, IOException;
    void remove(String encodedName);
}
