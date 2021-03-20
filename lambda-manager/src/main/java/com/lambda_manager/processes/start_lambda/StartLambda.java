package com.lambda_manager.processes.start_lambda;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.Tuple;

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
