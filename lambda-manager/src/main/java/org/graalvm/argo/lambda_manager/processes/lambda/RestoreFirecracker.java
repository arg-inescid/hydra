package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public abstract class RestoreFirecracker extends StartLambda {

    public RestoreFirecracker(Lambda lambda) {
        super(lambda);
    }

    protected List<String> prepareCommand(String imageName) {
        List<String> command = new ArrayList<>();
        LambdaConnection connection = lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/restore_firecracker.sh");
        command.add(String.valueOf(pid));
        command.add(connection.ip);
        command.add(lambda.getLambdaName());
        command.add(imageName);
        return command;
    }

}
