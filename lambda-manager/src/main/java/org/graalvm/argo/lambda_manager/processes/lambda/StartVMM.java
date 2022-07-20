package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import java.util.ArrayList;
import java.util.List;

// TODO - we need to deprecate this mode. All Jars will be compiled to SOs (and ran in graalvisor).
public class StartVMM extends StartLambda {

    public StartVMM(Lambda lambda, Function function) {
        super(lambda, function);
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
        command.add(function.getName());
        command.add(String.valueOf(pid));
        command.add(String.valueOf(Configuration.argumentStorage.getMemoryPool().getMaxMemory()));
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
        command.add(ENTRY_POINT_TAG + function.getEntryPoint());
        command.add(PORT_TAG + Configuration.argumentStorage.getLambdaPort());
        command.add("LD_LIBRARY_PATH=/lib:/lib64:/apps:/usr/local/lib");
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

			@Override
			public void finish(int exitCode) {
				if (exitCode != 0) {
		            // Need fallback to execution with Hotspot with agent.
		            lambda.getTimer().cancel();
		            if (function.getStatus() == FunctionStatus.READY) {
		                function.setStatus(FunctionStatus.NOT_BUILT_NOT_CONFIGURED);
		            }
		        }
			}
		};
    }
}
