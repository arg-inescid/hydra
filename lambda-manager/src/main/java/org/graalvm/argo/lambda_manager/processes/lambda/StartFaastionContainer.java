package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.List;

public class StartFaastionContainer extends StartContainer {

    private final Function function;
    private static final String ACTIVE_WAIT_CAP_TAG = "ACTIVE_WAIT_CAP=";

    public StartFaastionContainer(Lambda lambda, Function function) {
        super(lambda);
        this.function = function;
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = prepareCommand("faastion:latest");

        String runtime = function.getRuntime();
        if (Environment.FAASTION_RUNTIME.equals(runtime)) {
            command.add("--enable-lpi");
        }

        String capValue = getActiveWaitCapValue();
        if (capValue != null) {
            command.add(ACTIVE_WAIT_CAP_TAG + capValue);
        }

        return command;
    }

    private String getActiveWaitCapValue() {
        String benchmark = function.getBenchmarkName();
        if ("bf".equals(benchmark)) {
            return "360";
        } else if ("ms".equals(benchmark)) {
            return "360";
        } else if ("pr".equals(benchmark)) {
            return "360";
        } else if ("co".equals(benchmark)) {
            return "3600";
        } else if ("cl".equals(benchmark)) {
            return "2800";
        }
        return null;
    }
}
