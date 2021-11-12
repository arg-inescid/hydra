package com.lambda_manager.processes;

import com.lambda_manager.callbacks.DefaultCallback;
import com.lambda_manager.callbacks.OnProcessFinishCallback;
import com.lambda_manager.core.Environment;
import java.util.List;

import static com.lambda_manager.core.Environment.DEFAULT_FILENAME;

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
        return DEFAULT_FILENAME;
    }
}
