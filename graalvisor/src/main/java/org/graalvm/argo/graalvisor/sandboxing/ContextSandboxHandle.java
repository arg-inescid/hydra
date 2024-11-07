package org.graalvm.argo.graalvisor.sandboxing;

public class ContextSandboxHandle extends SandboxHandle {

    private final long functionHandle;
    private final long iThreadHandle;

    public ContextSandboxHandle(long functionHandle, long iThreadHandle) {
        this.functionHandle = functionHandle;
        this.iThreadHandle = iThreadHandle;
    }

    public long getIThreadHandle() {
        return iThreadHandle;
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