package org.graalvm.argo.lambda_manager.processes.lambda;

import static org.graalvm.argo.lambda_manager.core.Environment.LAMBDA_LOGS;
import static org.graalvm.argo.lambda_manager.core.Environment.MEMORY;
import static org.graalvm.argo.lambda_manager.core.Environment.OUTPUT;
import static org.graalvm.argo.lambda_manager.core.Environment.ERROR;
import java.io.File;
import java.nio.file.Paths;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.AbstractProcess;

public abstract class StartLambda extends AbstractProcess {

    protected static String TIMESTAMP_TAG = "lambda_timestamp=";
    protected static String PORT_TAG = "lambda_port=";
    protected final Lambda lambda;

    public StartLambda(Lambda lambda) {
        this.lambda = lambda;
        lambda.setLambdaID(pid);
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

    @Override
    protected String outputFilename() {
        String dirPath = Paths.get(LAMBDA_LOGS, lambda.getLambdaName()).toString();
        new File(dirPath).mkdirs();
        return Paths.get(dirPath, OUTPUT).toString();
    }

    @Override
    protected String errorFilename() {
        String dirPath = Paths.get(LAMBDA_LOGS, lambda.getLambdaName()).toString();
        new File(dirPath).mkdirs();
        return Paths.get(dirPath, ERROR).toString();
    }

    @Override
    protected String memoryFilename() {
        String dirPath = Paths.get(LAMBDA_LOGS, lambda.getLambdaName()).toString();
        new File(dirPath).mkdirs();
        return Paths.get(dirPath, MEMORY).toString();
    }
}
