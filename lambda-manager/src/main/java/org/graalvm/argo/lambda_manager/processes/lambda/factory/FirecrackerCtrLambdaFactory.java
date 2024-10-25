package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartOpenWhiskFirecrackerCtr;

public class FirecrackerCtrLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda) {
        return new StartHotspotWithAgentFirecrackerCtr(lambda);
    }

    @Override
    public StartLambda createHotspot(Lambda lambda) {
        return new StartHotspotFirecrackerCtr(lambda);
    }

    @Override
    public StartLambda createGraalvisor(Lambda lambda) {
        return new StartGraalvisorFirecrackerCtr(lambda);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda) {
        return new StartOpenWhiskFirecrackerCtr(lambda);
    }

    @Override
    public StartLambda createGraalvisorPgo(Lambda lambda) {
        throw new IllegalStateException("Graalvisor PGO is not yet supported in Firecracker-containerd mode.");
    }

    @Override
    public StartLambda createGraalvisorPgoOptimized(Lambda lambda) {
        throw new IllegalStateException("Graalvisor PGO Optimized is not yet supported in Firecracker-containerd mode.");
    }

    public StartLambda createGraalOS(Lambda lambda) {
        throw new UnsupportedOperationException("GraalOS not available with Firecracker containerd.");
    }

}
