package org.graalvm.argo.graalvisor.sandboxing;

public class ContextSnapshotSandboxHandle extends SandboxHandle {

    /**
     * Identifier of the svm instance used by this handle.
     */
    private final int svmid;
    /**
     * Pointer to the isolate thread structure (see NativeSandboxInterface.c).
     */
    private final long isolateThread;

    public ContextSnapshotSandboxHandle(int svmid, long isolateThread) {
        this.svmid = svmid;
        this.isolateThread = isolateThread;
    }

    public long getIsolateThread() {
        return this.isolateThread;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws Exception {
        return NativeSandboxInterface.svmEntrypoint(svmid, isolateThread);
    }

    @Override
    public String toString() {
        return Long.toString(isolateThread);
    }
}