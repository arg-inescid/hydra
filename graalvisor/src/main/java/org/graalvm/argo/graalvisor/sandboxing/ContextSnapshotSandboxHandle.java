package org.graalvm.argo.graalvisor.sandboxing;

public class ContextSnapshotSandboxHandle extends SandboxHandle {

    /**
     * Provider for this handle.
     */
    private final ContextSnapshotSandboxProvider provider;

    /**
     * Pointer to the isolate thread structure (see NativeSandboxInterface.c).
     */
    private final long isolateThread;

    public ContextSnapshotSandboxHandle(ContextSnapshotSandboxProvider provider, long isolateThread) {
        this.provider = provider;
        this.isolateThread = isolateThread;
    }

    public long getIsolateThread() {
        return this.isolateThread;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws Exception {
	 return provider.invoke(jsonArguments);
    }

    @Override
    public String toString() {
        return Long.toString(isolateThread);
    }
}
