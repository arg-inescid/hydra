package com.lambda_manager.processes.main;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.VMMBuiltCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static com.lambda_manager.core.Environment.LAMBDA_LOGS;

public class BuildVMM extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        this.outputFilename = outputFilename(lambda, configuration);

        command.add("/usr/bin/time");
        command.add("--append");
        command.add("--output=" + this.outputFilename);
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/build_vmm.sh");
        command.add(lambda.function.getName());
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        return new VMMBuiltCallback(lambda);
    }

    @Override
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        String dirPath = Paths.get(LAMBDA_LOGS, lambda.function.getName(), String.valueOf(lambda.lambda.getId())).toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdir();
        return outputFilename == null ?
                Paths.get(dirPath, "build_vmm.log").toString()
                : outputFilename;
    }
}
