package com.lambda_manager.processes.lambda;

import static com.lambda_manager.core.Environment.LAMBDA_LOGS;
import static com.lambda_manager.core.Environment.MEMORY;
import static com.lambda_manager.core.Environment.OUTPUT;
import java.io.File;
import java.nio.file.Paths;

import com.lambda_manager.collectors.meta_info.Lambda;
import com.lambda_manager.processes.AbstractProcess;

public abstract class StartLambda extends AbstractProcess {

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
        //noinspection ResultOfMethodCallIgnored
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
        //noinspection ResultOfMethodCallIgnored
        new File(dirPath).mkdirs();
        return Paths.get(dirPath, MEMORY).toString();
    }
}
