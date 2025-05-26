package org.graalvm.argo.graalvisor.sandboxing;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.graalvm.argo.graalvisor.function.NativeFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;

public class SnapshotSandboxProvider extends SandboxProvider {

    private AtomicBoolean warmedUp = new AtomicBoolean(false);
    /**
     * This number is used to identify a memory range where the svm instance will be restored.
     * Note 1: the same id should be used when checkpointing and restoring.
     * Note 2: we cannot host two functions with the same svmID at the same time.
     */
    private int svmID = 0;
    /**
     * Path to function library file.
     */
    private final String functionPath;
    /**
     * Paths where checkpoint saves svm instace state.
     */
    private final String metaSnapPath;
    private final String memSnapPath;

    public SnapshotSandboxProvider(PolyglotFunction function) {
        super(function);
        this.functionPath = ((NativeFunction) getFunction()).getPath();
        this.metaSnapPath = functionPath + ".metasnap"; // TODO - needs to be configurable.
        this.memSnapPath =  functionPath + ".memsnap";
    }

    public void setSVMID(int svmID) {
        System.out.println(String.format("Setting svmID %d in %s", svmID, functionPath));
        this.svmID = svmID;
    }

    public int getSVMID() {
        return svmID;
    }

    public String invoke(String jsonArguments) throws IOException {
        if (warmedUp.get()) {
            return NativeSandboxInterface.svmInvoke(svmID, jsonArguments);
        }

        synchronized (this) {
            return warmupProvider(1, 1, jsonArguments);
        }
    }

    @Override
    public String warmupProvider(int concurrency, int requests, String jsonArguments) throws IOException {
        // If already warm, just invoke.
        if (warmedUp.get()) {
            return NativeSandboxInterface.svmInvoke(svmID, jsonArguments);
        }

        synchronized (this) {
            if (new File(this.metaSnapPath).exists()) {
                System.out.println(String.format("Found %s, restoring svm.", this.metaSnapPath));
                String output = NativeSandboxInterface.svmRestore(svmID, functionPath, 1, 1, jsonArguments, metaSnapPath, memSnapPath);
                warmedUp.set(true);
                return output;
            } else {
                System.out.println(String.format("No snapshot found (%s), checkpointing svm after %d requests on %d concurrent threads.",
                    this.metaSnapPath, requests, concurrency));
                String output = NativeSandboxInterface.svmCheckpoint(svmID, functionPath, concurrency, requests, jsonArguments, metaSnapPath, memSnapPath);
                warmedUp.set(true);
                return output;
            }
        }
    }

    @Override
    public void loadProvider() throws IOException {
        // Nothin to be done at load time.
    }

    @Override
    public synchronized SandboxHandle createSandbox() throws IOException {
        // TODO - throw Exception if provider is not warmup yet.
        // TODO - for Truffle functions we attached because we want to create a new context inside
        // the same isolate. But for Java functions we could create a new isolate.
        return new SnapshotSandboxHandle(this);
    }

    @Override
    public void destroySandbox(SandboxHandle shandle) {
        // TODO - need to implement?
    }

    @Override
    public void unloadProvider() throws IOException {
        // TODO - need to implement?
    }

    @Override
    public String getName() {
        return "context-snapshot";
    }
}
