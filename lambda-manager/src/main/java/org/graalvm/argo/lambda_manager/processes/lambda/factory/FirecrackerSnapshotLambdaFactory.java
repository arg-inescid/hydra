package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreGraalvisorFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreHotspotFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreHotspotWithAgentFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.RestoreOpenWhiskFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;

public class FirecrackerSnapshotLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda, Function function) {
        return new RestoreHotspotWithAgentFirecracker(lambda, function);
    }

    @Override
    public StartLambda createHotspot(Lambda lambda, Function function) {
        return new RestoreHotspotFirecracker(lambda, function);
    }

    @Override
    public StartLambda createGraalvisor(Lambda lambda, Function function) {
        return new RestoreGraalvisorFirecracker(lambda, function);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        return new RestoreOpenWhiskFirecracker(lambda, function);
    }

}
