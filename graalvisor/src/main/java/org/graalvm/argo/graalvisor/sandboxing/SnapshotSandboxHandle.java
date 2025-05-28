package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;

public class SnapshotSandboxHandle extends SandboxHandle {

    // Native sandbox handle (pointer casted to long).
    private final long sandboxHandle;

    public SnapshotSandboxHandle(long sandboxHandle) {
        this.sandboxHandle = sandboxHandle;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws IOException {
        return NativeSandboxInterface.svmInvoke(this, jsonArguments);
    }

    @Override
    public String toString() {
        return Long.toString(sandboxHandle);
    }
}
