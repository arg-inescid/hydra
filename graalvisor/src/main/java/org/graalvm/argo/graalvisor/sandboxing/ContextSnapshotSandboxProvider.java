package org.graalvm.argo.graalvisor.sandboxing;

import java.io.File;
import java.io.IOException;

import org.graalvm.argo.graalvisor.function.NativeFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;

public class ContextSnapshotSandboxProvider extends SandboxProvider {

    private boolean warmedUp = false;
    /**
     * This number is used to identify a memory range where the svm instance will be restored.
     * Note 1: the same id should be used when checkpointing and restoring.
     * Note 2: we cannot host two functions with the same svmID at the same time.
     */
    private final int svmID = 0; // TODO - this should be set dynamically.
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
        this.metaSnapPath = functionPath + ".metasnap"; // TODO - needs to be configurable.
        this.memSnapPath =  functionPath + ".memsnap";
    }

    private static String invoke(int svmID, String jsonArguments) {
        long isolateThread = NativeSandboxInterface.svmAttachThread(svmID);
        String output = NativeSandboxInterface.svmEntrypoint(svmID, isolateThread, jsonArguments);
        NativeSandboxInterface.svmDetachThread(svmID, isolateThread);
        return output;
    }

    public synchronized String warmupProvider(int concurrency, int requests, String jsonArguments) throws IOException {
        if (warmedUp) {
            return invoke(svmID, jsonArguments);
        } else if (new File(this.metaSnapPath).exists()) {
            System.out.println(String.format("Found %s, restoring svm.", this.metaSnapPath));
            NativeSandboxInterface.svmRestore(svmID, functionPath, metaSnapPath, memSnapPath);
            warmedUp = true;
            return invoke(svmID, jsonArguments);
        } else {
            System.out.println(String.format("No snapshot found (%s), checkpointing svm after %d requests on %d concurrent threads.",
                this.metaSnapPath, requests, concurrency));
            String output = NativeSandboxInterface.svmCheckpoint(svmID, functionPath, concurrency, requests, jsonArguments, metaSnapPath, memSnapPath);
            warmedUp = true;
            return output;
        }
    }

    @Override
    public void loadProvider() throws IOException {
        // Nothin to be done at load time.
    }

    @Override
    public synchronized SandboxHandle createSandbox() throws Exception {
        // TODO - throw Exception if provider is not warmup yet.
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
