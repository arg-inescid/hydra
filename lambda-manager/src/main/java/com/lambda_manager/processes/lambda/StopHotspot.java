package com.lambda_manager.processes.lambda;

import com.lambda_manager.collectors.meta_info.Function;
import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.processes.AbstractProcess;
import com.lambda_manager.utils.LambdaTuple;

import java.util.List;

public class StopHotspot extends AbstractProcess {

    @Override
    protected List<String> makeCommand(LambdaTuple<Function, Lambda> lambda) {
        return null;
    }

    @Override
    protected String outputFilename(LambdaTuple<Function, Lambda> lambda) {
        return null;
    }
}
