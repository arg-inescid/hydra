package com.lambda_manager.code_writer.impl;

import com.lambda_manager.code_writer.FunctionWriter;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.utils.LambdaTuple;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("unused")
public class DefaultFunctionWriter implements FunctionWriter {

    @Override
    public LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> upload(LambdaInstancesInfo lambdaInstancesInfo,
                                                                       String encodedName, byte[] lambdaCode)
            throws ErrorUploadingNewLambda, IOException {

        int id = lambdaInstancesInfo.getId();
        LambdaInstanceInfo lambdaInstanceInfo = new LambdaInstanceInfo(id);
        LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda = new LambdaTuple<>(lambdaInstancesInfo, lambdaInstanceInfo);

        File newSrcDir = new File("src/codebase/" + encodedName);
        if(newSrcDir.mkdirs()) {
            File lambdaCodeFile = new File("src/codebase/" + encodedName + "/" + encodedName + ".jar");
            if(lambdaCodeFile.createNewFile()) {
                FileOutputStream fileOutputStream = new FileOutputStream(lambdaCodeFile);
                fileOutputStream.write(lambdaCode);
                fileOutputStream.close();
            } else {
                throw new ErrorUploadingNewLambda("Error during uploading new lambda [ " + lambda.list.getName() + " ]!");
            }
        }
        lambdaInstancesInfo.getAvailableInstances().add(lambdaInstanceInfo);
        return lambda;
    }

    @Override
    public void remove(String encodedName) {
        File dir = new File("src/codebase/" + encodedName);
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
