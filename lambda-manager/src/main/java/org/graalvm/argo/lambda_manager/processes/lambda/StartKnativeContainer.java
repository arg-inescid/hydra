package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.List;

public class StartKnativeContainer extends StartContainer {

    private final Function function;

    public StartKnativeContainer(Lambda lambda, Function function) {
        super(lambda);
        this.function = function;
    }

    @Override
    protected List<String> makeCommand() {
        return prepareCommand(function.getFunctionCode());
    }
}
