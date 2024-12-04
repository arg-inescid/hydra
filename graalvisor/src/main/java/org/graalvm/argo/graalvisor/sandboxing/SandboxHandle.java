package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;

public abstract class SandboxHandle {

    public abstract String invokeSandbox(String jsonArguments) throws IOException;

    public void destroyHandle() throws IOException {
        // default implementation;
    }

    @Override
    public abstract String toString();
}