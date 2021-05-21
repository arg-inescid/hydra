package com.lambda_manager.processes.lambda;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.core.Configuration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.util.List;

public class StartLambda extends AbstractProcess {

    private StartLambda nextToSpawn;

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda) {
        this.nextToSpawn = Configuration.optimizer.whomToSpawn(lambda);
        return nextToSpawn.makeCommand(lambda);
    }

    @Override
    protected OnProcessFinishCallback callback(LambdaTuple<Function, Lambda> lambda) {
        return nextToSpawn.callback(lambda);
    }

    @Override
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda) {
        return nextToSpawn.outputFilename(lambda);
    }

    @Override
    protected String memoryFilename(LambdaTuple<Function, Lambda> lambda) {
        return nextToSpawn.memoryFilename(lambda);
    }
}
