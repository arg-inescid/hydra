package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;
import java.io.File;

import org.graalvm.argo.graalvisor.function.NativeFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;
import org.graalvm.word.WordFactory;
import org.graalvm.nativeimage.Isolate;
import org.graalvm.nativeimage.IsolateThread;
import com.oracle.svm.graalvisor.api.GraalVisorAPI;

public class ContextSandboxProvider extends SandboxProvider {

    private GraalVisorAPI graalvisorAPI;

    private Isolate isolate;

    public ContextSandboxProvider(PolyglotFunction function) {
        super(function);
    }

    public GraalVisorAPI getGraalvisorAPI() {
        return this.graalvisorAPI;
    }

    public String warmupProvider(String jsonArguments) throws IOException {
        String functionPath = ((NativeFunction) getFunction()).getPath();
        String metaSnapPath = "/tmp/metadata.snap";
        String memSnapPath = "/tmp/memory.snap";
        if (new File(metaSnapPath).exists()) {
            //this.isolate = WordFactory.pointer(NativeSandboxInterface.restoreSVM(metaSnapPath, memSnapPath,));
            NativeSandboxInterface.restoreSVM(metaSnapPath, memSnapPath);
            return "Need to implement real invoke";
        } else {
            return NativeSandboxInterface.checkpointSVM(
                functionPath,
                null,
                metaSnapPath,
                memSnapPath);
        }
    }

    @Override
    public void loadProvider() throws IOException { // TODO - use warmup for checkpoint and load for restore?
        this.graalvisorAPI = new GraalVisorAPI(((NativeFunction) getFunction()).getPath());
        IsolateThread isolateThread = graalvisorAPI.createIsolate();
        this.isolate = graalvisorAPI.getIsolate(isolateThread);
        graalvisorAPI.detachThread(isolateThread);
    }

    @Override
    public SandboxHandle createSandbox() {
        IsolateThread isolateThread = graalvisorAPI.attachThread(isolate);
        return new ContextSandboxHandle(this, isolateThread);
    }

    @Override
    public void destroySandbox(SandboxHandle shandle) {
        graalvisorAPI.detachThread(((ContextSandboxHandle)shandle).getIsolateThread());
    }

    @Override
    public void unloadProvider() throws IOException {
        IsolateThread isolateThread = graalvisorAPI.attachThread(isolate);
        graalvisorAPI.tearDownIsolate(isolateThread);
        graalvisorAPI.close();
    }

    @Override
    public String getName() {
        return "context";
    }
}
