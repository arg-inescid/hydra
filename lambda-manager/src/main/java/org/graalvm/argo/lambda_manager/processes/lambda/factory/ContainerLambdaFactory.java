package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.*;

public class ContainerLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda) {
        return new StartHotspotWithAgentContainer(lambda);
    }

    @Override
    public StartLambda createHotspot(Lambda lambda) {
        return new StartHotspotContainer(lambda);
    }

    @Override
    public StartLambda createHydra(Lambda lambda) {
        return new StartHydraContainer(lambda);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        return new StartOpenWhiskContainer(lambda, function);
    }

    @Override
    public StartLambda createHydraPgo(Lambda lambda) {
        return new StartHydraPgoContainer(lambda);
    }

    @Override
    public StartLambda createHydraPgoOptimized(Lambda lambda) {
        return new StartHydraPgoOptimizedContainer(lambda);
    }

    public StartLambda createGraalOS(Lambda lambda) {
        return new StartGraalOSContainer(lambda);
    }

    @Override
    public StartLambda createKnative(Lambda lambda, Function function) {
        return new StartKnativeContainer(lambda, function);
    }

}
