package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalOSNative;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;

public class NativeLambdaFactory extends AbstractLambdaFactory {
    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda) {
        throw new IllegalStateException("HotSpot with Agent is not yet supported in native mode.");
    }

    @Override
    public StartLambda createHotspot(Lambda lambda) {
        throw new IllegalStateException("HotSpot is not yet supported in native mode.");
    }

    @Override
    public StartLambda createHydra(Lambda lambda) {
        throw new IllegalStateException("Hydra is not yet supported in native mode.");
    }

    @Override
    public StartLambda createHydraPgo(Lambda lambda) {
        throw new IllegalStateException("Hydra PGO is not yet supported in native mode.");
    }

    @Override
    public StartLambda createHydraPgoOptimized(Lambda lambda) {
        throw new IllegalStateException("Hydra PGO Optimized is not yet supported in native mode.");
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        throw new IllegalStateException("OpenWhisk is not yet supported in native mode.");
    }

    @Override
    public StartLambda createGraalOS(Lambda lambda) {
        return new StartGraalOSNative(lambda);
    }

    @Override
    public StartLambda createKnative(Lambda lambda, Function function) {
        throw new IllegalStateException("Knative is not yet supported in native mode.");
    }
}
