package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;

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
    public StartLambda createGraalvisor(Lambda lambda) {
        return new StartGraalvisorContainer(lambda);
    }

    @Override
    public StartLambda createOpenWhisk(Lambda lambda) {
        throw new IllegalStateException("OpenWhisk is not yet supported in container mode.");
    }

}
