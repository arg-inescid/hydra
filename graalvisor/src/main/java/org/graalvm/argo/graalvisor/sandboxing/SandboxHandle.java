package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SandboxHandle {

    public static final AtomicInteger SANDBOX_ID_COUNTER = new AtomicInteger(0);
    protected final int sandboxId;

    public SandboxHandle() {
        this.sandboxId = SANDBOX_ID_COUNTER.getAndIncrement();
    }

    public abstract String invokeSandbox(String jsonArguments) throws IOException;

    public void destroyHandle() throws IOException {
        // default implementation;
    }

    @Override
    public abstract String toString();

    public int getSandboxId() {
        return this.sandboxId;
    }

    public String getSandboxTmpDirectoryPath() {
        String path = "/tmp/sandbox-" + this.sandboxId;
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            // TODO: maybe some robust handling?
            e.printStackTrace();
        }
        return path;
    }
}
