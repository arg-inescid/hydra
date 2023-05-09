package org.graalvm.argo.lambda_manager.processes.lambda;

import static org.graalvm.argo.lambda_manager.core.Environment.RUN_LOG;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Function;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;

public abstract class StartHotspotWithAgent extends StartLambda {

    public StartHotspotWithAgent(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

            @Override
            public void finish(int exitCode) {
                function.setLastAgentPID(lambda.getLambdaID());
                function.setStatus(FunctionStatus.NOT_BUILT_CONFIGURED);
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
