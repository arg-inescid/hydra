package com.lambda_manager.code_writer.impl;

import com.lambda_manager.code_writer.CodeWriter;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.exceptions.user.ErrorUploadingNewLambda;
import com.lambda_manager.utils.Tuple;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("unused")
public class DefaultCodeWriter implements CodeWriter {

    @Override
    public Tuple<LambdaInstancesInfo, LambdaInstanceInfo> upload(LambdaInstancesInfo lambdaInstancesInfo,
                                                                 String encodedName, byte[] lambdaCode)
            throws ErrorUploadingNewLambda, IOException {

        int id = lambdaInstancesInfo.getId();
        LambdaInstanceInfo lambdaInstanceInfo = new LambdaInstanceInfo(id);
        lambdaInstancesInfo.setId(id + 1);
        Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda = new Tuple<>(lambdaInstancesInfo, lambdaInstanceInfo);

        File newSrcDir = new File("src/lambdas/" + encodedName);
        if(newSrcDir.mkdirs()) {
            File lambdaCodeFile = new File("src/lambdas/" + encodedName + "/" + encodedName + ".jar");
            if(lambdaCodeFile.createNewFile()) {
                FileOutputStream fileOutputStream = new FileOutputStream(lambdaCodeFile);
                fileOutputStream.write(lambdaCode);
                fileOutputStream.close();
            } else {
                throw new ErrorUploadingNewLambda("Error during uploading new lambda [ " + lambda.list.getName() + " ]!");
            }
            File newConfigDir = new File("src/lambdas/" + encodedName + "/config");
            if(!newConfigDir.mkdirs()) {
                throw new ErrorUploadingNewLambda("Error during uploading new lambda [ " + lambda.list.getName() + " ]!");
            }
        }
        lambdaInstancesInfo.getAvailableInstances().add(lambdaInstanceInfo);
        return lambda;
    }

    @Override
    public void remove(String encodedName) {
        File dir = new File("src/lambdas/" + encodedName);
        if(dir.exists()) {
            purgeDirectory(dir);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void purgeDirectory(File dir) {
        //noinspection ConstantConditions
        for (File file: dir.listFiles()) {
            if (file.isDirectory())
                purgeDirectory(file);
            file.delete();
        }
        dir.delete();
    }
}
