package com.lambda_manager.callbacks;

import com.lambda_manager.core.Function;
import com.lambda_manager.optimizers.FunctionStatus;

public class BuildVMMCallback implements OnProcessFinishCallback {

    private final Function function;

    public BuildVMMCallback(Function function) {
        this.function = function;
    }

    @Override
    public void finish(int exitCode) {
        function.setStatus(FunctionStatus.BUILT);
    }
}
