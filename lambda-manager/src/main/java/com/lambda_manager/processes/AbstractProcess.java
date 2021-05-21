package com.lambda_manager.processes;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.DefaultCallback;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.core.Environment;
import com.lambda_manager.utils.LambdaTuple;

import java.util.List;

import static com.lambda_manager.core.Environment.DEFAULT_FILENAME;

public abstract class AbstractProcess {

    public final ProcessBuilder build(LambdaTuple<Function, Lambda> lambda) {
        return new ProcessBuilder(pid(lambda), makeCommand(lambda), callback(lambda), outputFilename(lambda));
    }

    protected abstract List<String> makeCommand(LambdaTuple<Function, Lambda> lambda);

    protected OnProcessFinishCallback callback(LambdaTuple<Function, Lambda> lambda) {
        return new DefaultCallback();
    }

    protected abstract String outputFilename(LambdaTuple<Function, Lambda> lambda);

    protected String memoryFilename(LambdaTuple<Function, Lambda> lambda) {
        return DEFAULT_FILENAME;
    }

    private long pid(LambdaTuple<Function, Lambda> lambdaTuple) {
        long nextPID = Environment.pid();
        if(lambdaTuple != null) {
            lambdaTuple.lambda.setPid(nextPID);
        }
        return nextPID;
    }
}
