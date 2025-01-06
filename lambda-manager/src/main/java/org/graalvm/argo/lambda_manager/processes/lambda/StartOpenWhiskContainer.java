package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartOpenWhiskContainer extends StartContainer {

    public StartOpenWhiskContainer(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        // TODO (flex containers): Perhaps only set memory limits for OW lambdas.
        return prepareCommand(lambda.getExecutionMode().getOpenWhiskContainerImage());
    }
}
