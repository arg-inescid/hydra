package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class StartCustomRuntime extends StartLambda {

    public StartCustomRuntime(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();

        if (function.getRuntime().equals(Environment.GRAALVISOR_DOCKER_RUNTIME)) {
            // Reuse custom runtime's infrastructure for Graalvisor in firecracker-containerd mode.
            lambda.setExecutionMode(LambdaExecutionMode.GRAALVISOR_CONTAINERD);
        } else {
            lambda.setExecutionMode(LambdaExecutionMode.CUSTOM);
        }
        LambdaConnection connection = lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_cruntime.sh");
        command.add(function.getName());
        command.add(String.valueOf(pid));
        command.add(String.valueOf(function.getMemory()));
        command.add(connection.ip);
        command.add(connection.tap);
        command.add(Configuration.argumentStorage.getGateway());
        command.add(Configuration.argumentStorage.getMask());
        if (Configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("--noconsole");
        }
        command.add(function.getRuntime());
        String lambdaId = generateLambdaId();
        lambda.setCustomRuntimeId(lambdaId);
        command.add(lambdaId);
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

    private static final String AB = "0123456789abcdef";
    private static SecureRandom rnd = new SecureRandom();
    private static final int ID_LEN = 32;

    static String generateLambdaId() {
        StringBuilder sb = new StringBuilder(ID_LEN);
        for (int i = 0; i < ID_LEN; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }
}
