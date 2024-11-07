package org.graalvm.argo.graalvisor.sandboxing;

public class IsolateSandboxHandle extends SandboxHandle {

    // Native function handle (pointer casted to long).
    private final long functionHandle;
    // Native isolate thread handle (pointer casted to long).
    private final long iThreadHandle;

    public IsolateSandboxHandle(long functionHandle, long iThreadHandle) {
        this.functionHandle = functionHandle;
        this.iThreadHandle = iThreadHandle;
        NativeSandboxInterface.createNativeIsolateSandbox(); // TODO - keep?
    }

    public long getIThreadHandle() {
        return this.iThreadHandle;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws Exception {
        return NativeSandboxInterface.invokeSandbox(functionHandle, iThreadHandle, jsonArguments);
    }

    @Override
    public String toString() {
        return Long.toString(iThreadHandle);
    }
}