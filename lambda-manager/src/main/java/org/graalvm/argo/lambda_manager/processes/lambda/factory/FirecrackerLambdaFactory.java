package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentFirecracker;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartOpenWhiskFirecracker;

public class FirecrackerLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda, Function function) {
        return new StartHotspotWithAgentFirecracker(lambda, function);
    }

    @Override
    public StartLambda createHotspot(Lambda lambda, Function function) {
        return new StartHotspotFirecracker(lambda, function);
    }

    @Override
    public StartLambda createGraalvisor(Lambda lambda, Function function) {
        return new StartGraalvisorFirecracker(lambda, function);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        return new StartOpenWhiskFirecracker(lambda, function);
    }

}
