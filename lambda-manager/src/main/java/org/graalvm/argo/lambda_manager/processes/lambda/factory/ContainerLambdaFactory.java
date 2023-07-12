package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;

public class ContainerLambdaFactory extends AbstractLambdaFactory {

    @Override
    public StartLambda createHotspotWithAgent(Lambda lambda, Function function) {
        return new StartHotspotWithAgentContainer(lambda, function);
    }

    @Override
    public StartLambda createHotspot(Lambda lambda, Function function) {
        return new StartHotspotContainer(lambda, function);
    }

    @Override
    public StartLambda createGraalvisor(Lambda lambda, Function function) {
        return new StartGraalvisorContainer(lambda, function);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        throw new IllegalStateException("OpenWhisk is not yet supported in container mode.");
    }

}
