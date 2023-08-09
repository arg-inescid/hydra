package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.List;

public class StartHotspotFirecrackerCtr extends StartFirecrackerCtr {

    public StartHotspotFirecrackerCtr(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand(Environment.HOTSPOT_DOCKER_RUNTIME);
    }
}
