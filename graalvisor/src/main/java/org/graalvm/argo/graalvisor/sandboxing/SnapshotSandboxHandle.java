package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;

public class SnapshotSandboxHandle extends SandboxHandle {

    /**
     * Provider for this handle.
     */
    private final SnapshotSandboxProvider provider;

    /**
     * Pointer to the isolate thread structure (see NativeSandboxInterface.c).
     */
    private final long isolateThread;

    public SnapshotSandboxHandle(SnapshotSandboxProvider provider, long isolateThread) {
        this.provider = provider;
        this.isolateThread = isolateThread;
    }

    public long getIsolateThread() {
        return this.isolateThread;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws IOException {
	 return provider.invoke(jsonArguments);
    }

    @Override
    public String toString() {
        return Long.toString(isolateThread);
    }
}
