package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;

public abstract class AbstractLambdaFactory {

    public abstract StartLambda createHotspotWithAgent(Lambda lambda);

    public abstract StartLambda createHotspot(Lambda lambda);

    public abstract StartLambda createGraalvisor(Lambda lambda);

    public abstract StartLambda createGraalvisorPgo(Lambda lambda);

    public abstract StartLambda createGraalvisorPgoOptimized(Lambda lambda);

    public abstract StartLambda createOpenWhisk(Lambda lambda, Function function);

    public abstract StartLambda createGraalOS(Lambda lambda);

    public abstract StartLambda createKnative(Lambda lambda, Function function);

    public abstract StartLambda createFaastion(Lambda lambda, Function function);
}
