package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartOpenWhiskFirecracker extends StartFirecracker {

    public StartOpenWhiskFirecracker(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand(lambda.getExecutionMode().getOpenWhiskVMImage());
    }

}
