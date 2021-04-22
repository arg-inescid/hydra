package com.lambda_manager.processes.main;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.NativeImageBuiltCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static com.lambda_manager.utils.Constants.LAMBDA_LOGS;

public class BuildNativeImage extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        this.outputFilename = outputFilename(lambda, configuration);

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.outputFilename);
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/build_vmm.sh");
        command.add(lambda.list.getName());
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new NativeImageBuiltCallback(lambda);
    }

    @Override
    protected String outputFilename(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        String dirPath = Paths.get(LAMBDA_LOGS, String.valueOf(lambda.instance.getId()), lambda.list.getName()).toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdir();
        return outputFilename == null ?
                Paths.get(dirPath, "build_native_image.log").toString()
                : outputFilename;
    }
}
