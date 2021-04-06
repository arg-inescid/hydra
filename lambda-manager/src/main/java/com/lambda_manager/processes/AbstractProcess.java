package com.lambda_manager.processes;

import com.lambda_manager.callbacks.impl.DefaultCallback;
import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractProcess {

    protected List<String> command; // TODO - delete this field. We don't need it.
    protected String processOutputFile; // TODO - delete this field. We don't need it.

    public final ProcessBuilder build(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new ProcessBuilder(makeCommand(lambda, configuration), callback(lambda, configuration),
                processOutputFile(lambda, configuration));
    }

    protected abstract List<String> makeCommand(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);

    protected OnProcessFinishCallback callback(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new DefaultCallback();
    }

    protected abstract String processOutputFile(Tuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                                    LambdaManagerConfiguration configuration);

    // TODO - this method should be deleted once we delete the fields.
    protected void clearPreviousState() {
        this.command = new ArrayList<>();
        this.processOutputFile = null;
    }
}
