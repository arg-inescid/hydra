package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentFirecrackerCtr;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartOpenWhiskFirecrackerCtr;

public class FirecrackerCtrLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda, Function function) {
        return new StartHotspotWithAgentFirecrackerCtr(lambda, function);
    }

    @Override
    public StartLambda createHotspot(Lambda lambda, Function function) {
        return new StartHotspotFirecrackerCtr(lambda, function);
    }

    @Override
    public StartLambda createGraalvisor(Lambda lambda, Function function) {
        return new StartGraalvisorFirecrackerCtr(lambda, function);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        return new StartOpenWhiskFirecrackerCtr(lambda, function);
    }

}
