package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.graalvm.argo.lambda_manager.core.Environment.*;

public class StartHotspot extends StartLambda {

    public StartHotspot(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();

        lambda.setExecutionMode(LambdaExecutionMode.HOTSPOT);
        LambdaConnection connection = lambda.getConnection();

        command.add("/usr/bin/time");
        command.add("--append");
        command.add(String.format("--output=%s", memoryFilename()));
        command.add("-v");
        command.add("bash");
        command.add("src/scripts/start_hotspot.sh");
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
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(ENTRY_POINT_TAG + function.getEntryPoint());
        command.add(PORT_TAG + Configuration.argumentStorage.getLambdaPort());
        return command;
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

			@Override
			public void finish(int exitCode) {
		        String sourceFilename = Paths.get(Environment.CODEBASE, "/", lambda.getLambdaName(), RUN_LOG).toString();
		        String destinationFilename = outputFilename();
				File sourceFile = new File(sourceFilename);
		        try (FileInputStream fileInputStream = new FileInputStream(sourceFile);
		                        FileWriter fileWriter = new FileWriter(destinationFilename, true)) {
		            byte[] data = new byte[(int) sourceFile.length()];
		            fileInputStream.read(data);
		            fileWriter.write(new String(data, StandardCharsets.UTF_8));
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
			}
		};
    }
}
