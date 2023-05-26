package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

import java.util.List;

public class StartHotspotWithAgentFirecrackerCtr extends StartFirecrackerCtr {

    public StartHotspotWithAgentFirecrackerCtr(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
        lambda.setExecutionMode(LambdaExecutionMode.HOTSPOT_W_AGENT);
        return prepareCommand(Environment.HOTSPOT_AGENT_DOCKER_RUNTIME);
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

            @Override
            public void finish(int exitCode) {
                function.setLastAgentPID(lambda.getLambdaID());
                function.setStatus(FunctionStatus.NOT_BUILT_CONFIGURED);
                lambda.resetRegisteredInLambda();
            }
        };
    }
}
