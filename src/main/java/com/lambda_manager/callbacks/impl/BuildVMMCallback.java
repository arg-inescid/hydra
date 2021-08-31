package com.lambda_manager.callbacks.impl;

import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.collectors.meta_info.Function;
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
