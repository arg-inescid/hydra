package com.lambda_manager.function_writer.impl;

import com.lambda_manager.core.Environment;
import com.lambda_manager.function_writer.FunctionWriter;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.exceptions.user.ErrorUploadingLambda;
import com.lambda_manager.utils.LambdaTuple;
import com.lambda_manager.utils.Messages;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

@SuppressWarnings("unused")
public class DefaultFunctionWriter implements FunctionWriter {

    @Override
    public LambdaTuple<Function, Lambda> upload(Function function,
                                                String encodedName, byte[] functionCode)
            throws ErrorUploadingLambda, IOException {

        Lambda lambdaInstanceInfo = new Lambda();
        LambdaTuple<Function, Lambda> lambda = new LambdaTuple<>(function, lambdaInstanceInfo);

        String functionDir = Paths.get(Environment.CODEBASE, encodedName).toString();
        File newSrcDir = new File(functionDir);
        if(newSrcDir.mkdirs()) {
            File functionFile = new File(Paths.get(functionDir, encodedName + ".jar").toString());
            if(functionFile.createNewFile()) {
                FileOutputStream fileOutputStream = new FileOutputStream(functionFile);
                fileOutputStream.write(functionCode);
                fileOutputStream.close();
            } else {
                throw new ErrorUploadingLambda(String.format(Messages.ERROR_FUNCTION_UPLOAD, lambda.function.getName()));
            }
        }
        function.getAvailableLambdas().add(lambdaInstanceInfo);
        return lambda;
    }

    @Override
    public void remove(String encodedName) {
        File dir = new File(Paths.get(Environment.CODEBASE, encodedName).toString());
        if(dir.exists()) {
            purgeDirectory(dir, true);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void purgeDirectory(File dir, boolean deleteDir) {
        //noinspection ConstantConditions
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                purgeDirectory(file, deleteDir);
            }
            file.delete();
        }
        if(deleteDir) {
            dir.delete();
        }
    }
}
