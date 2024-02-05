package org.graalvm.argo.graalvisor.sandboxing;

import org.graalvm.argo.graalvisor.function.PolyglotFunction;

import java.io.IOException;

public class PgoSandboxProvider extends SandboxProvider {

    public PgoSandboxProvider(PolyglotFunction function) {
        super(function);
    }

    @Override
    public String getName() {
        return "pgo";
    }

    @Override
    public void loadProvider() throws IOException {

    }

    @Override
    public SandboxHandle createSandbox() throws Exception {
        return new PgoSandboxHandle(this);
    }

    @Override
    public void destroySandbox(SandboxHandle shandle) throws IOException {

    }

    @Override
    public void unloadProvider() throws IOException {

    }
}
