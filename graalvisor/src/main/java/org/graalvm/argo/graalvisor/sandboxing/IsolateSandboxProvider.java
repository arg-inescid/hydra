package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;

import org.graalvm.argo.graalvisor.base.NativeFunction;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import com.oracle.svm.graalvisor.api.GraalVisorAPI;
import com.oracle.svm.graalvisor.types.GuestIsolateThread;

public class IsolateSandboxProvider extends SandboxProvider {

    private GraalVisorAPI graalVisorAPI;

    public IsolateSandboxProvider(PolyglotFunction function) {
        super(function);
    }

    public GraalVisorAPI getGraalvisorAPI() {
        return this.graalVisorAPI;
    }

    @Override
    public void loadProvider() throws IOException {
        this.graalVisorAPI = new GraalVisorAPI(((NativeFunction) getFunction()).getPath());
    }

    @Override
    public SandboxHandle createSandbox() {
        GuestIsolateThread isolateThread = graalVisorAPI.createIsolate();
        return new IsolateSandboxHandle(this, isolateThread);
    }

    @Override
    public void destroySandbox(SandboxHandle shandle) {
        IsolateSandboxHandle ipshandle = (IsolateSandboxHandle) shandle;
        graalVisorAPI.tearDownIsolate((GuestIsolateThread) ipshandle.getIsolateThread());
    }

    @Override
    public void unloadProvider() throws IOException {
        graalVisorAPI.close();
    }

    @Override
    public String getName() {
        return "isolate";
    }
}
