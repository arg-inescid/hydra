package org.graalvm.argo.lambda_manager.processes;

import org.graalvm.argo.lambda_manager.callbacks.DefaultCallback;
import org.graalvm.argo.lambda_manager.callbacks.OnProcessFinishCallback;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.List;

public abstract class AbstractProcess {

    /**
     * The process identifier (not to be confused with OS pid).
     */
    protected long pid;

    public final ProcessBuilder build() {
        this.pid = Environment.pid();
        return new ProcessBuilder(this.getClass().getName(), pid, makeCommand(), callback(), outputFilename());
    }

    protected abstract List<String> makeCommand();

    protected OnProcessFinishCallback callback() {
        return new DefaultCallback();
    }

    protected abstract String outputFilename();

    protected String memoryFilename() {
        return Environment.DEFAULT_FILENAME;
    }
}
