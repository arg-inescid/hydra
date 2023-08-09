package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;

public abstract class StartContainer extends StartLambda {

    public StartContainer(Lambda lambda) {
        super(lambda);
    }
}
