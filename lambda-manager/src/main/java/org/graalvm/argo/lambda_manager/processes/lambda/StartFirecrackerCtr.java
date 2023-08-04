package org.graalvm.argo.lambda_manager.processes.lambda;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public abstract class StartFirecrackerCtr extends StartLambda {

    private static final String AB = "0123456789abcdef";
    private static SecureRandom rnd = new SecureRandom();
    private static final int ID_LEN = 32;

    public StartFirecrackerCtr(Lambda lambda) {
        super(lambda);
    }

    protected List<String> prepareCommand(String runtimeName) {
        List<String> command = new ArrayList<>();
        LambdaConnection connection = lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_cruntime.sh");
        command.add(String.valueOf(pid));
        command.add(String.valueOf(Configuration.argumentStorage.getLambdaMemory()));
        command.add(connection.ip);
        command.add(connection.tap);
        command.add(Configuration.argumentStorage.getGateway());
        command.add(Configuration.argumentStorage.getMask());
        if (Configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("--noconsole");
        }
        command.add(runtimeName);
        String lambdaId = generateLambdaId();
        lambda.setCustomRuntimeId(lambdaId);
        command.add(lambdaId);
        command.add(lambda.getLambdaName());
        return command;
    }

    private final String generateLambdaId() {
        StringBuilder sb = new StringBuilder(ID_LEN);
        for (int i = 0; i < ID_LEN; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }

}
