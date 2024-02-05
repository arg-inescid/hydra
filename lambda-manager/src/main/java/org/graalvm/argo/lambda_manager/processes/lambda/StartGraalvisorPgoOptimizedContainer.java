package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED;

public class StartGraalvisorPgoOptimizedContainer extends StartContainer {

    public StartGraalvisorPgoOptimizedContainer(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        lambda.setExecutionMode(GRAALVISOR_PGO_OPTIMIZED);

        return List.of(
                "/usr/bin/time",
                "--append",
                format("--output=%s", memoryFilename()),
                "-v",
                "bash",
                "src/scripts/start_graalvisor_container.sh",
                valueOf(pid),
                lambda.getLambdaName(),
                TIMESTAMP_TAG + currentTimeMillis(),
                PORT_TAG + lambda.getConnection().port,
                "LD_LIBRARY_PATH=/lib:/lib64:/tmp/apps:/usr/local/lib"
        );
    }
}
