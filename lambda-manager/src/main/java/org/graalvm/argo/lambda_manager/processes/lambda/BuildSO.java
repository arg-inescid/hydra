package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.processes.AbstractProcess;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.graalvm.argo.lambda_manager.core.Environment.LAMBDA_LOGS;

public class BuildSO extends AbstractProcess {

    private final Function function;

    public BuildSO(Function function) {
        this.function = function;
    }

    @Override
    protected List<String> makeCommand() {
        function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/build_so.sh");
        command.add(function.getName());
        command.add(String.valueOf(function.getLastAgentPID()));
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

            @Override
            public void finish(int exitCode) {
                function.setStatus(FunctionStatus.READY);
            }
        };
    }

    @Override
    protected String outputFilename() {
        return Paths.get(LAMBDA_LOGS, "build_app_so_" + function.getName() + ".log").toString();
    }
}
