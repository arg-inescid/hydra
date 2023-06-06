package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;

public class StartHotspotWithAgentFirecracker extends StartFirecracker {

    public StartHotspotWithAgentFirecracker(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        function.setStatus(FunctionStatus.CONFIGURING_OR_BUILDING);
        lambda.setExecutionMode(LambdaExecutionMode.HOTSPOT_W_AGENT);

        List<String> command = prepareCommand("src/scripts/start_firecracker.sh");
        command.add(lambda.getLambdaName());
        command.add("hotspot-agent");
        return command;
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
