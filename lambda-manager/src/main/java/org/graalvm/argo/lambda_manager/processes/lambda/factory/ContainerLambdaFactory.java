package org.graalvm.argo.lambda_manager.processes.lambda.factory;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalOSContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorPgoContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartGraalvisorPgoOptimizedContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartHotspotWithAgentContainer;
import org.graalvm.argo.lambda_manager.processes.lambda.StartLambda;
import org.graalvm.argo.lambda_manager.processes.lambda.StartOpenWhiskContainer;

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
    public StartLambda createOpenWhisk(Lambda lambda, Function function) {
        return new StartOpenWhiskContainer(lambda, function);
    }

    @Override
    public StartLambda createGraalvisorPgo(Lambda lambda) {
        return new StartGraalvisorPgoContainer(lambda);
    }

    @Override
    public StartLambda createGraalvisorPgoOptimized(Lambda lambda) {
        return new StartGraalvisorPgoOptimizedContainer(lambda);
    }

    public StartLambda createGraalOS(Lambda lambda) {
        return new StartGraalOSContainer(lambda);
    }

}
