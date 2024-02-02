package org.graalvm.argo.lambda_manager.processes;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.processes.lambda.OnProcessFinishCallback;

import java.util.List;

public abstract class AbstractProcess {

    /**
     * The process identifier (not to be confused with OS pid).
     */
    protected final long pid;

    public AbstractProcess() {
        this.pid = Environment.pid();
    }

    public final ProcessBuilder build() {
        return new ProcessBuilder(this.getClass().getName(), pid, makeCommand(), callback(), outputFilename(), errorFilename());
    }

    protected abstract List<String> makeCommand();

    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

			@Override
			public void finish(int exitCode) { }
		};
    }

    protected abstract String outputFilename();

    protected String errorFilename() {
        return outputFilename();
    }

    protected String memoryFilename() {
        return Environment.DEFAULT_FILENAME;
    }
}
