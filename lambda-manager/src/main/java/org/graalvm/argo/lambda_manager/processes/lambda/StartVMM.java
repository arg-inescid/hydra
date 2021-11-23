package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.callbacks.OnProcessFinishCallback;
import org.graalvm.argo.lambda_manager.callbacks.VMMCallback;
import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartVMM extends StartLambda {

    public StartVMM(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();

        lambda.setExecutionMode(LambdaExecutionMode.NATIVE_IMAGE);
        ConnectionTriplet<String, String, RxHttpClient> connectionTriplet = lambda.getConnectionTriplet();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_vmm.sh");
        command.add(lambda.getFunction().getName());
        command.add(String.valueOf(pid));
        command.add(Configuration.argumentStorage.getMemorySpace());
        command.add(connectionTriplet.ip);
        command.add(connectionTriplet.tap);
        command.add(Configuration.argumentStorage.getGateway());
        command.add(Configuration.argumentStorage.getMask());
        if (Configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("--noconsole");
        }
        command.add(String.valueOf(System.currentTimeMillis()));
        command.add(lambda.getFunction().getEntryPoint());
        if (lambda.getFunction().getArguments() != null) {
            Collections.addAll(command, lambda.getFunction().getArguments().split(","));
        }
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new VMMCallback(lambda);
    }

    @Override
    public String getLambdaDirectory() {
        return Environment.VMM;
    }
}
