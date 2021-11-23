package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.callbacks.BuildVMMCallback;
import org.graalvm.argo.lambda_manager.callbacks.OnProcessFinishCallback;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.processes.AbstractProcess;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.graalvm.argo.lambda_manager.core.Environment.BUILD_VMM;
import static org.graalvm.argo.lambda_manager.core.Environment.LAMBDA_LOGS;

public class BuildVMM extends AbstractProcess {

    private final Function function;

    public BuildVMM(Function function) {
        this.function = function;
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/build_vmm.sh");
        command.add(function.getName());
        command.add(String.valueOf(function.getLastAgentPID()));
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new BuildVMMCallback(function);
    }

    @Override
    protected String outputFilename() {
        String dirPath = Paths.get(LAMBDA_LOGS, function.getName(), BUILD_VMM).toString();
        new File(dirPath).mkdir();
        return Paths.get(dirPath, "output.log").toString();
    }
}
