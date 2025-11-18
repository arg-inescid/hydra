package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHydraFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartOpenWhiskFirecracker;

public class FirecrackerLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda) {
        return new StartHotspotWithAgentFirecracker(lambda);
    }

    @Override
    public StartLambda createHotspot(Lambda lambda) {
        return new StartHotspotFirecracker(lambda);
    }

    @Override
    public StartLambda createHydra(Lambda lambda) {
        return new StartHydraFirecracker(lambda);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        return new StartOpenWhiskFirecracker(lambda);
    }

    @Override
    public StartLambda createHydraPgo(Lambda lambda) {
        throw new IllegalStateException("Hydra PGO is not yet supported in Firecracker mode.");
    }

    @Override
    public StartLambda createHydraPgoOptimized(Lambda lambda) {
        throw new IllegalStateException("Hydra PGO Optimized is not yet supported in Firecracker mode.");
    }

    public StartLambda createGraalOS(Lambda lambda) {
        throw new UnsupportedOperationException("GraalOS not available with Firecracker.");
    }

    @Override
    public StartLambda createKnative(Lambda lambda, Function function) {
        throw new IllegalStateException("Knative is not yet supported in Firecracker mode.");
    }

}
