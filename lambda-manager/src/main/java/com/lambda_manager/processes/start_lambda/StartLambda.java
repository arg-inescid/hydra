package com.lambda_manager.processes.start_lambda;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.util.List;

public class StartLambda extends AbstractProcess {

    private StartLambda nextToSpawn;

    @Override
    protected List<String> makeCommand(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration state) {
        this.nextToSpawn = state.optimizer.whomToSpawn(lambda, state);
        return nextToSpawn.makeCommand(lambda, state);
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return nextToSpawn.callback(lambda, configuration);
    }

    @Override
    protected String outputFilename(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return nextToSpawn.outputFilename(lambda, configuration);
    }

    @Override
    protected String memoryFilename(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return nextToSpawn.memoryFilename(lambda, configuration);
    }

    @Override
    protected long pid() {
        return nextToSpawn.pid();
    }
}
