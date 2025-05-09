package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;

public class SnapshotSandboxHandle extends SandboxHandle {

    /**
     * Provider for this handle.
     */
    private final SnapshotSandboxProvider provider;

    public SnapshotSandboxHandle(SnapshotSandboxProvider provider) {
        this.provider = provider;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws IOException {
	 return provider.invoke(jsonArguments);
    }

    @Override
    public String toString() {
        return Integer.toString(provider.getSVMID());
    }
}
