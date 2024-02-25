package org.graalvm.argo.graalvisor.sandboxing;

import java.io.File;
import java.io.IOException;

import org.graalvm.argo.graalvisor.function.NativeFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;

public class ContextSnapshotSandboxProvider extends SandboxProvider {

    private final int svmID;
    private final String functionPath;
    private final String metaSnapPath = "/tmp/metadata.snap";
    private final String memSnapPath = "/tmp/memory.snap";

    public ContextSnapshotSandboxProvider(PolyglotFunction function) {
        super(function);
        this.svmID = 0;
        this.functionPath = ((NativeFunction) getFunction()).getPath();
    }

    public synchronized String warmupProvider(String jsonArguments) throws IOException {
        if (new File(this.metaSnapPath).exists()) {
            NativeSandboxInterface.svmRestore(svmID, metaSnapPath, memSnapPath);
            long isolateThread = NativeSandboxInterface.svmAttachThread(svmID);
            return NativeSandboxInterface.svmEntrypoint(svmID, isolateThread);
        } else {
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
