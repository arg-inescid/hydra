package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.NetworkConnection;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.argo.lambda_manager.core.Configuration;

public abstract class StartFirecracker extends StartLambda {

    public StartFirecracker(Lambda lambda) {
        super(lambda);
    }

    protected List<String> prepareCommand(String imageName) {
        List<String> command = new ArrayList<>();
        NetworkConnection connection = (NetworkConnection) lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_firecracker.sh");
        command.add(String.valueOf(pid));
        command.add(String.valueOf(Configuration.argumentStorage.getMaxMemory()));
        command.add(connection.getIp());
        command.add(connection.getTap());
        command.add(Configuration.argumentStorage.getGateway());
        command.add(Configuration.argumentStorage.getMask());
        if (Configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("--noconsole");
        }
        command.add(lambda.getLambdaName());
        command.add(imageName);
        return command;
    }
}
