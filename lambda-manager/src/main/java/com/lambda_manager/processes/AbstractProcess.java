package com.lambda_manager.processes;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.callbacks.impl.DefaultCallback;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.utils.Constants;
import com.lambda_manager.utils.LambdaTuple;

import java.util.ArrayList;
import java.util.List;

import static com.lambda_manager.utils.Constants.DEFAULT_FILENAME;

public abstract class AbstractProcess {

    protected List<String> command; // TODO - delete this field. We don't need it.
    protected String outputFilename; // TODO - delete this field. We don't need it.
    protected String memoryFilename; // TODO - delete this field. We don't need it.
    protected long pid = -1;

    public final ProcessBuilder build(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new ProcessBuilder(makeCommand(lambda, configuration), callback(lambda, configuration),
                outputFilename(lambda, configuration), pid());
    }

    protected abstract List<String> makeCommand(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration);

    protected OnProcessFinishCallback callback(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda, LambdaManagerConfiguration configuration) {
        return new DefaultCallback();
    }

    protected abstract String outputFilename(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                                             LambdaManagerConfiguration configuration);

    protected String memoryFilename(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda,
                                    LambdaManagerConfiguration configuration) {
        return DEFAULT_FILENAME;
    }

    // TODO - this method should be deleted once we delete the fields.
    protected void clearPreviousState() {
        this.command = new ArrayList<>();
        this.outputFilename = null;
        this.memoryFilename = null;
        this.pid = -1;
    }

    protected long pid() {
        this.pid = Constants.pid();
        return this.pid;
    }
}
