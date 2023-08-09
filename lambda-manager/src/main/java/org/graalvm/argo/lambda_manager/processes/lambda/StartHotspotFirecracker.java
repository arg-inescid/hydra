package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartHotspotFirecracker extends StartFirecracker {

    public StartHotspotFirecracker(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand("hotspot");
    }

}
