package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.List;

public class StartGraalvisorFirecrackerCtr extends StartFirecrackerCtr {

    public StartGraalvisorFirecrackerCtr(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand(Environment.GRAALVISOR_DOCKER_RUNTIME);
    }
}
