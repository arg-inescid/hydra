package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;

import org.graalvm.argo.graalvisor.base.NativeFunction;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;

import com.oracle.svm.graalvisor.api.GraalVisorAPI;
import com.oracle.svm.graalvisor.types.GuestIsolateThread;

public class RuntimeSandboxHandle extends SandboxHandle {

    private final RuntimeSandboxProvider rsProvider;

    private final GraalVisorAPI graalvisorAPI;

    private final IsolateThread isolateThread;

    public RuntimeSandboxHandle(RuntimeSandboxProvider rsProvider) throws IOException {
        NativeFunction function = (NativeFunction) rsProvider.getFunction();
        this.graalvisorAPI = new GraalVisorAPI(function.getPath());
        this.isolateThread = graalvisorAPI.createIsolate();
        this.rsProvider = rsProvider;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws Exception {
        PolyglotFunction function = rsProvider.getFunction();
        return graalvisorAPI.invokeFunction((GuestIsolateThread) isolateThread, function.getEntryPoint(), jsonArguments);
    }

    @Override
    public void destroyHandle() throws IOException {
        graalvisorAPI.tearDownIsolate((GuestIsolateThread) isolateThread);
        graalvisorAPI.close();
    }

    @Override
    public String toString() {
        return Long.toString(Isolates.getIsolate(isolateThread).rawValue());
    }

}
