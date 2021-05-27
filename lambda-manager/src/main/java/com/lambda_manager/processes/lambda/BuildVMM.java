package com.lambda_manager.processes.lambda;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.BuildVMMCallback;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.lambda_manager.core.Environment.BUILD_VMM;
import static com.lambda_manager.core.Environment.LAMBDA_LOGS;

public class BuildVMM extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda) {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/build_vmm.sh");
        command.add(lambda.function.getName());
        command.add(String.valueOf(lambda.function.getLastAgentPID()));
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<Function, Lambda> lambda) {
        return new BuildVMMCallback(lambda);
    }

    @Override
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda) {
        String dirPath = Paths.get(LAMBDA_LOGS, lambda.function.getName(), BUILD_VMM).toString();
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdir();
        return Paths.get(dirPath, "output.log").toString();
    }
}
