package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.processes.AbstractProcess;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static org.graalvm.argo.lambda_manager.core.Environment.LAMBDA_LOGS;
import static org.graalvm.argo.lambda_manager.optimizers.FunctionStatus.PGO_OPTIMIZED_BUILDING;
import static org.graalvm.argo.lambda_manager.optimizers.FunctionStatus.PGO_OPTIMIZED_READY;

public class BuildNativeImagePgoOptimized extends AbstractProcess {

    private final Function function;

    public BuildNativeImagePgoOptimized(Function function) {
        this.function = function;
    }

    @Override
    protected List<String> makeCommand() {
        return Stream
                .of("bash",
                        "src/scripts/build_native_image_pgo_optimized.sh",
                        function.getName(),
                        valueOf(function.getLastAgentPID()),
                        function.getEntryPoint())
                .collect(toList());
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return exitCode -> function.setStatus(PGO_OPTIMIZED_READY);
    }

    @Override
    protected String outputFilename() {
        return Paths.get(LAMBDA_LOGS, "build_app_native_image_pgo_optimized" + function.getName() + ".log").toString();
    }
}
