package com.serverless_demo.processes;

import com.serverless_demo.callbacks.impl.DefaultCallback;
import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.utils.Tuple;

import java.util.List;

public abstract class AbstractProcess {
    protected List<String> command;

    public final ProcessBuilder build(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new ProcessBuilder(makeCommand(lambda, configuration), destroyForcibly(), callback(lambda, configuration));
    }

    public abstract List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);

    public boolean destroyForcibly() {
        return false;
    }

    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new DefaultCallback();
    }
}
