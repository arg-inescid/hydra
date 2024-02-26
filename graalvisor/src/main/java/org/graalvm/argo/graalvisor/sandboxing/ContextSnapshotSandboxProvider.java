package org.graalvm.argo.graalvisor.sandboxing;

import java.io.File;
import java.io.IOException;

import org.graalvm.argo.graalvisor.function.NativeFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;

public class ContextSnapshotSandboxProvider extends SandboxProvider {

    /**
     * This number is used to identify a memory range where the svm instance will be restored.
     * Note 1: the same id should be used when checkpointing and restoring.
     * Note 2: we cannot host two functions with the same svmID at the same time.
     */
    private final int svmID = 0;
    /**
     * Path to function library file.
     */
    private final String functionPath;
    /**
     * Paths where checkpoint saves svm instace state.
     */
    private final String metaSnapPath;
    private final String memSnapPath;

    public ContextSnapshotSandboxProvider(PolyglotFunction function) {
        super(function);
        this.functionPath = ((NativeFunction) getFunction()).getPath();
        this.metaSnapPath = functionPath + ".metasnap";
        this.memSnapPath =  functionPath + ".memsnap";
    }

    public synchronized String warmupProvider(String jsonArguments) throws IOException {
        if (new File(this.metaSnapPath).exists()) {
            System.out.println(String.format("Found %s, restoring svm.", this.metaSnapPath));
            NativeSandboxInterface.svmRestore(svmID, metaSnapPath, memSnapPath);
            long isolateThread = NativeSandboxInterface.svmAttachThread(svmID);
            return NativeSandboxInterface.svmEntrypoint(svmID, isolateThread);
        } else {
            System.out.println(String.format("No snapshot found (%s), checkpointing svm.", this.metaSnapPath));
            return NativeSandboxInterface.svmCheckpoint(svmID, functionPath, jsonArguments, metaSnapPath, memSnapPath);
        }
    }

    @Override
    public void loadProvider() throws IOException {
        // Nothin to be done at load time.
    }

    @Override
    public synchronized SandboxHandle createSandbox() throws Exception {
        long isolateThread = NativeSandboxInterface.svmAttachThread(svmID);
        return new ContextSnapshotSandboxHandle(this.svmID, isolateThread);
    }

    @Override
    public void destroySandbox(SandboxHandle shandle) {
        NativeSandboxInterface.svmDetachThread(svmID, ((ContextSnapshotSandboxHandle)shandle).getIsolateThread());
    }

    @Override
    public void unloadProvider() throws IOException {
        // TODO - need to implement.
    }

    @Override
    public String getName() {
        return "context-snapshot";
    }
}
