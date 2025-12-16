package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class RestoreHydraFirecracker extends RestoreFirecracker {

    public RestoreHydraFirecracker(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand("hydra");
    }

}
