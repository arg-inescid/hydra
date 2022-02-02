package org.graalvm.argo.lambda_manager.processes.lambda;

import static org.graalvm.argo.lambda_manager.core.Environment.LAMBDA_LOGS;
import static org.graalvm.argo.lambda_manager.core.Environment.MEMORY;
import static org.graalvm.argo.lambda_manager.core.Environment.OUTPUT;
import java.io.File;
import java.nio.file.Paths;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.processes.AbstractProcess;

public abstract class StartLambda extends AbstractProcess {

    protected static String TIMESTAMP_TAG = "lambda_timestamp=";
    protected static String ENTRY_POINT_TAG = "lambda_entry_point=";
    protected static String PORT_TAG = "lambda_port=";
    protected final Lambda lambda;

    public StartLambda(Lambda lambda) {
        this.lambda = lambda;
    }

    public abstract String getLambdaDirectory();

    @Override
    protected String outputFilename() {
        String dirPath = Paths.get(
                        LAMBDA_LOGS,
                        lambda.getFunction().getName(),
                        String.format(getLambdaDirectory(), pid))
                        .toString();
        new File(dirPath).mkdirs();
        return Paths.get(dirPath, OUTPUT).toString();
    }

    @Override
    protected String memoryFilename() {
        String dirPath = Paths.get(
                        LAMBDA_LOGS,
                        lambda.getFunction().getName(),
                        String.format(getLambdaDirectory(), pid))
                        .toString();
        new File(dirPath).mkdirs();
        return Paths.get(dirPath, MEMORY).toString();
    }
}
