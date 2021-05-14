package com.lambda_manager.function_writer;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.exceptions.user.ErrorUploadingLambda;
import com.lambda_manager.utils.LambdaTuple;

import java.io.IOException;

public interface FunctionWriter {
    LambdaTuple<Function, Lambda> upload(Function function, String encodedName,
                                         byte[] functionCode) throws ErrorUploadingLambda, IOException;
    void remove(String encodedName);
}
