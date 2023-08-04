package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class RestoreGraalvisorFirecracker extends RestoreFirecracker {

    public RestoreGraalvisorFirecracker(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand("graalvisor");
    }

}
