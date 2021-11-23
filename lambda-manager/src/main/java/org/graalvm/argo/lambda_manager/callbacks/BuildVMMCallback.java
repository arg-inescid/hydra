package org.graalvm.argo.lambda_manager.callbacks;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;

public class BuildVMMCallback implements OnProcessFinishCallback {

    private final Function function;

    public BuildVMMCallback(Function function) {
        this.function = function;
    }

    @Override
    public void finish(int exitCode) {
        function.setStatus(FunctionStatus.BUILT);
    }
}
