package org.graalvm.argo.graalvisor.sandboxing;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.argo.graalvisor.function.NativeFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;
import org.graalvm.argo.graalvisor.utils.JsonUtils;

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

    /**
     * In snapshotted sandboxes, we have a 'base' sandbox that is created on restore.
     * This sandbox is used to create additional sandboxes, should not be destroyed but
     * can be used for normal invocations.
     */
    private final SnapshotSandboxHandle sandboxHandle = new SnapshotSandboxHandle();
    /**
     * Number of sandboxes handles returned from createSandbox.
     */
    private AtomicInteger sandboxHandleCounter = new AtomicInteger(0);

    public SnapshotSandboxProvider(PolyglotFunction function) {
        super(function);
        this.functionPath = ((NativeFunction) getFunction()).getPath();
        this.metaSnapPath = functionPath + ".metasnap";
        this.memSnapPath =  functionPath + ".memsnap";
    }

    public void setSVMID(int svmID) throws IOException {

        if (this.svmID != 0) {
            throw new IOException(String.format("Error: svmID was already set to %d for function %s", svmID, function.getName()));
        }

        this.svmID = svmID;
    }

    @Override
    public String warmupProvider(int concurrency, int requests, String jsonArguments) throws IOException {
        // Extending the arguments JSON object to include sandbox-specific tmp directory.
        jsonArguments = JsonUtils.appendTmpDirectoryKey(jsonArguments, sandboxHandle.initSandboxTmpDirectory());

        // If already warm, just invoke.
        if (warmedUp.get()) {
            return NativeSandboxInterface.svmInvoke(sandboxHandle, jsonArguments);
        }

        synchronized (this) {
            if (warmedUp.get()) {
                // The other thread has already warmed up in the meantime, just invoke.
                return NativeSandboxInterface.svmInvoke(sandboxHandle, jsonArguments);
            } else if (new File(this.metaSnapPath).exists()) {
                // A snapshot was found, we are restoring.
                System.out.println(String.format("Found %s, restoring svm.", this.metaSnapPath));
                String output = NativeSandboxInterface.svmRestore(
                    svmID, sandboxHandle, jsonArguments, functionPath, metaSnapPath, memSnapPath);
                warmedUp.set(true);
                return output;
            } else {
                // A snapshot was NOT found, we are checkpointing.
                System.out.println(String.format("No snapshot found (%s), checkpointing svm after %d requests on %d concurrent threads.",
                    this.metaSnapPath, requests, concurrency));
                String output = NativeSandboxInterface.svmCheckpoint(
                    svmID, sandboxHandle, concurrency, requests, jsonArguments, functionPath, metaSnapPath, memSnapPath);
                warmedUp.set(true);
                return output;
            }
        }
    }

    @Override
    public boolean isWarm() {
        return this.warmedUp.get();
    }

    @Override
    public void loadProvider() throws IOException {
        // Nothin to be done at load time.
    }

    @Override
    public synchronized SandboxHandle createSandbox() throws IOException {
        if (warmedUp.get()) {
            if (sandboxHandleCounter.getAndIncrement() == 0) {
                return this.sandboxHandle;
            } else {
                SnapshotSandboxHandle clone = new SnapshotSandboxHandle();
                // TODO - for Java applications we may not want to reuse the isolate.
                NativeSandboxInterface.svmClone(this.sandboxHandle, clone, true);
                return clone;
            }
        } else {
            throw new IOException(String.format("Snapshot sandbox provider not initialized for function %s", function.getName()));
        }
    }

    @Override
    public void destroySandbox(SandboxHandle shandle) throws IOException {
        sandboxHandleCounter.getAndDecrement();
        System.out.println("Snapshot sandbox handle destroy has not been implemented yet. Ignoring..");
        shandle.destroyHandle();
    }

    @Override
    public void unloadProvider() throws IOException {
        throw new IOException("Snapshot sandbox provider unloading is not supported yet...");
    }

    @Override
    public String getName() {
        return "context-snapshot";
    }
}
