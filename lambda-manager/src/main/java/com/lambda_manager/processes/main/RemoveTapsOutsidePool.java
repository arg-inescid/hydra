package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.utils.LambdaTuple;

import java.util.List;

public class RemoveTapsOutsidePool extends RemoveTapsFromPool {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda, LambdaManagerConfiguration configuration) {
        clearPreviousState();
        command.add("bash");
        command.add("src/scripts/remove_remain_taps.sh");
        return command;
    }
}
