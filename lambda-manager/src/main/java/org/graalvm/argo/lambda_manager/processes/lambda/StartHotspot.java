package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;

public abstract class StartHotspot extends StartLambda {

    public StartHotspot(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

            @Override
            public void finish(int exitCode) {
                lambda.resetRegisteredInLambda();
            }
        };
    }

}
