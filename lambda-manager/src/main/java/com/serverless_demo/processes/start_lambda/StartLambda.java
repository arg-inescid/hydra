package com.serverless_demo.processes.start_lambda;

import com.serverless_demo.callbacks.OnProcessFinishCallback;
import com.serverless_demo.collectors.lambda_info.LambdaInstanceInfo;
import com.serverless_demo.collectors.lambda_info.LambdaInstancesInfo;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.processes.AbstractProcess;
import com.serverless_demo.utils.Tuple;

import java.util.List;

public class StartLambda extends AbstractProcess {
    private StartLambda nextToSpawn;

    @Override
    public List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration state) {
        this.nextToSpawn = state.optimizer.whomToSpawn(lambda, state);
        return nextToSpawn.makeCommand(lambda, state);
    }

    @Override
    public boolean destroyForcibly() {
        return nextToSpawn.destroyForcibly();
    }

    @Override
    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return nextToSpawn.callback(lambda, configuration);
    }
}
