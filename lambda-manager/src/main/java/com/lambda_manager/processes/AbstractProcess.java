package com.lambda_manager.processes;

import com.lambda_manager.callbacks.impl.DefaultCallback;
import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.utils.Tuple;

import java.util.List;

public abstract class AbstractProcess {
    protected List<String> command;

    public final ProcessBuilder build(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new ProcessBuilder(makeCommand(lambda, configuration), destroyForcibly(), callback(lambda, configuration),
                processOutputFile(lambda, configuration));
    }

    public abstract List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);

    public boolean destroyForcibly() {
        return false;
    }

    public OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new DefaultCallback();
    }

    public String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                                    LambdaManagerConfiguration configuration) {
        if(lambda == null) {
            return "dummy.dat";
        }
        return "src/lambdas/" + lambda.list.getName() + "/outputs";
    }
}
