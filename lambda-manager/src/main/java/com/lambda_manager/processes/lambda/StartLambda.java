package com.lambda_manager.processes.lambda;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.util.List;

public class StartLambda extends AbstractProcess {

    private StartLambda nextToSpawn;

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration state) {
        this.nextToSpawn = state.optimizer.whomToSpawn(lambda, state);
        return nextToSpawn.makeCommand(lambda, state);
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        return nextToSpawn.callback(lambda, configuration);
    }

    @Override
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        return nextToSpawn.outputFilename(lambda, configuration);
    }

    @Override
    protected String memoryFilename(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        return nextToSpawn.memoryFilename(lambda, configuration);
    }

    @Override
    protected long pid() {
        return nextToSpawn.pid();
    }
}
