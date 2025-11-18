package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;

public abstract class AbstractLambdaFactory {

    public abstract StartLambda createHotspotWithAgent(Lambda lambda);

    public abstract StartLambda createHotspot(Lambda lambda);

    public abstract StartLambda createHydra(Lambda lambda);

    public abstract StartLambda createHydraPgo(Lambda lambda);

    public abstract StartLambda createHydraPgoOptimized(Lambda lambda);

    public abstract StartLambda createOpenWhisk(Lambda lambda, Function function);

    public abstract StartLambda createGraalOS(Lambda lambda);

    public abstract StartLambda createKnative(Lambda lambda, Function function);
}
