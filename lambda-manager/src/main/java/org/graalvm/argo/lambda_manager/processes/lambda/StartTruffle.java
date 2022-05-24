package org.graalvm.argo.lambda_manager.processes.lambda;

import io.micronaut.http.client.RxHttpClient;
import org.graalvm.argo.lambda_manager.callbacks.OnProcessFinishCallback;
import org.graalvm.argo.lambda_manager.callbacks.TruffleCallback;
import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;

import java.util.ArrayList;
import java.util.List;

public class StartTruffle extends StartLambda {

    public StartTruffle(Lambda lambda) {
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
        command.add("src/scripts/start_truffle.sh");
        command.add(lambda.getFunction().getName());
        command.add(String.valueOf(pid));
        // TODO: Should we put separate field in config for truffle memory or should it be like proportionally bigger then regular?
        command.add(String.valueOf(512 * 3));
        command.add(connectionTriplet.ip);
        command.add(connectionTriplet.tap);
        command.add(Configuration.argumentStorage.getGateway());
        command.add(Configuration.argumentStorage.getMask());
        if (Configuration.argumentStorage.isLambdaConsoleActive()) {
            command.add("--console");
        } else {
            command.add("--noconsole");
        }
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(PORT_TAG + Configuration.argumentStorage.getLambdaPort());
        command.add("LD_LIBRARY_PATH=/lib:/lib64:/apps:/usr/local/lib");
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new TruffleCallback(lambda);
    }

    @Override
    public String getLambdaDirectory() {
        return Environment.VMM;
    }
}
