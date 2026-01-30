package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.NetworkConnection;

import java.util.ArrayList;
import java.util.List;

public class StartGraalOSNative extends StartLambda {

    public StartGraalOSNative(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        NetworkConnection connection = (NetworkConnection) lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_graalos_native.sh");
        command.add(String.valueOf(connection.getPort()));
        command.add(lambda.getLambdaName());
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

            @Override
            public void finish(int exitCode) {
                lambda.resetRegisteredInLambda();
            }
        };
    }
}
