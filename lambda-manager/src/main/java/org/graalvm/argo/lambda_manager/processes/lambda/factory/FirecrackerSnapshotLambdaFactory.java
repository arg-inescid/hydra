package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreHydraFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreHotspotFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreHotspotWithAgentFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreOpenWhiskFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;

public class FirecrackerSnapshotLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda) {
        return new RestoreHotspotWithAgentFirecracker(lambda);
    }

    @Override
    public StartLambda createHotspot(Lambda lambda) {
        return new RestoreHotspotFirecracker(lambda);
    }

    @Override
    public StartLambda createHydra(Lambda lambda) {
        return new RestoreHydraFirecracker(lambda);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        return new RestoreOpenWhiskFirecracker(lambda);
    }

    @Override
    public StartLambda createHydraPgo(Lambda lambda) {
        throw new IllegalStateException("Hydra PGO is not yet supported in Firecracker-snapshot mode.");
    }

    @Override
    public StartLambda createHydraPgoOptimized(Lambda lambda) {
        throw new IllegalStateException("Hydra PGO Optimized is not yet supported in Firecracker-snapshot mode.");
    }

    public StartLambda createGraalOS(Lambda lambda) {
        throw new UnsupportedOperationException("GraalOS not available with Firecracker snapshots.");
    }

    @Override
    public StartLambda createKnative(Lambda lambda, Function function) {
        throw new IllegalStateException("Knative not available with Firecracker snapshots.");
    }

}
