package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;

public abstract class StartFirecracker extends StartLambda {

    public StartFirecracker(Lambda lambda, Function function) {
        super(lambda, function);
    }
}
