package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreGraalvisorFirecracker;
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
    public StartLambda createGraalvisor(Lambda lambda) {
        return new RestoreGraalvisorFirecracker(lambda);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda) {
        return new RestoreOpenWhiskFirecracker(lambda);
    }

    @Override
    public StartLambda createGraalvisorPgo(Lambda lambda) {
        throw new IllegalStateException("Graalvisor PGO is not yet supported in Firecracker-snapshot mode.");
    }

    @Override
    public StartLambda createGraalvisorPgoOptimized(Lambda lambda) {
        throw new IllegalStateException("Graalvisor PGO Optimized is not yet supported in Firecracker-snapshot mode.");
    }

    public StartLambda createGraalOS(Lambda lambda) {
        throw new UnsupportedOperationException("GraalOS not available with Firecracker snapshots.");
    }

}
