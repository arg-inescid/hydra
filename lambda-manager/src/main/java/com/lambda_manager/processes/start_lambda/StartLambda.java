package com.lambda_manager.processes.start_lambda;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.util.List;

// TODO - shouldn't this be an abstract class?
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
    protected String processOutputFile(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return nextToSpawn.processOutputFile(lambda, configuration);
    }
}
