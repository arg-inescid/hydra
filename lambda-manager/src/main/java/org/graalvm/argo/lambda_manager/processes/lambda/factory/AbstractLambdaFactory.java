package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;

public abstract class AbstractLambdaFactory {

    public abstract StartLambda createHotspotWithAgent(Lambda lambda, Function function);

    public abstract StartLambda createHotspot(Lambda lambda, Function function);

    public abstract StartLambda createGraalvisor(Lambda lambda, Function function);

    public abstract StartLambda createOpenWhisk(Lambda lambda, Function function);

}
