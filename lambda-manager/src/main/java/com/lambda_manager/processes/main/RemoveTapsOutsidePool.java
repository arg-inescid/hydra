package com.lambda_manager.processes.main;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.utils.LambdaTuple;

import java.util.ArrayList;
import java.util.List;

public class RemoveTapsOutsidePool extends RemoveTapsFromPool {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda) {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_remain_taps.sh");
        return command;
    }
}
