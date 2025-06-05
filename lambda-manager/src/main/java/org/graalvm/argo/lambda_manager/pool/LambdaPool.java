package org.graalvm.argo.lambda_manager.pool;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public interface LambdaPool {
    void setUp();

    LambdaConnection nextLambdaConnection();

    Lambda getLambda(LambdaExecutionMode mode, Function function);

    void disposeLambda(Lambda lambda);

    void tearDown() throws InterruptedException;
}
