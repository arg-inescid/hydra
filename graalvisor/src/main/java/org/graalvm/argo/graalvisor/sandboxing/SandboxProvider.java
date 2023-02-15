package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;

import org.graalvm.argo.graalvisor.base.PolyglotFunction;

public abstract class SandboxProvider {

    private final PolyglotFunction function;

    public SandboxProvider(PolyglotFunction function) {
        this.function = function;
    }

    public PolyglotFunction getFunction() {
        return this.function;
    }

    public abstract String getName();

    public abstract void loadProvider() throws IOException;

    public abstract SandboxHandle createSandbox() throws IOException;

    public abstract void destroySandbox(SandboxHandle shandle) throws IOException;

    public abstract void unloadProvider() throws IOException;

}