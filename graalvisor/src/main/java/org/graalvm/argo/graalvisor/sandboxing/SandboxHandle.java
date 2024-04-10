package org.graalvm.argo.graalvisor.sandboxing;

public abstract class SandboxHandle {

    public String ns = null; // TODO - temporary.

    public abstract String invokeSandbox(String jsonArguments) throws Exception;

    public void destroyHandle() throws Exception {
        // default implementation;
    }

    @Override
    public abstract String toString();
}