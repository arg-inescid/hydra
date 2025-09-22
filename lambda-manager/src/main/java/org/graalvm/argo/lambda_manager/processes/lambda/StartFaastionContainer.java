package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.List;

public class StartFaastionContainer extends StartContainer {

    private final Function function;

    public StartFaastionContainer(Lambda lambda, Function function) {
        super(lambda);
        this.function = function;
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = prepareCommand("faastion:latest");
        String runtime = function.getRuntime();
        if (Environment.FAASTLANE_RUNTIME.equals(runtime)) {
            command.add("--enable-early-booking");
        } else if (Environment.FAASTION_LPI_RUNTIME.equals(runtime)) {
            command.add("--enable-lpi");
        }
        return command;
    }
}
