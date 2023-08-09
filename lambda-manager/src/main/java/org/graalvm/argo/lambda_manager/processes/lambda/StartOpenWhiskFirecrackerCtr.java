package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.List;

public class StartOpenWhiskFirecrackerCtr extends StartFirecrackerCtr {

    public StartOpenWhiskFirecrackerCtr(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand(Environment.OPENWHISK_DOCKER_RUNTIME);
    }
}
