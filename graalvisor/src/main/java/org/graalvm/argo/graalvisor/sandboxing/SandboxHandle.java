package org.graalvm.argo.graalvisor.sandboxing;

public abstract class SandboxHandle {

    protected final SandboxProvider sprovider;

    public SandboxHandle(SandboxProvider sprovider) {
        this.sprovider = sprovider;
    }

    public abstract String invokeSandbox(String jsonArguments) throws Exception;

    @Override
    public abstract String toString();
}