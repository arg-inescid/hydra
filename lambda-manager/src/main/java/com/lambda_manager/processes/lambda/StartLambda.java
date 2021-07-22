package com.lambda_manager.processes.lambda;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.processes.AbstractProcess;

public abstract class StartLambda extends AbstractProcess {

    protected final Lambda lambda;

    public StartLambda(Lambda lambda) {
        this.lambda = lambda;
    }
}
