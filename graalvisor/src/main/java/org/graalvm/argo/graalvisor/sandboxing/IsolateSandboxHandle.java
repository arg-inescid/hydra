package org.graalvm.argo.graalvisor.sandboxing;

import org.graalvm.argo.graalvisor.function.PolyglotFunction;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import com.oracle.svm.graalvisor.types.GuestIsolateThread;

public class IsolateSandboxHandle extends SandboxHandle {

    private final IsolateSandboxProvider isProvider;

    private final IsolateThread isolateThread;

    public IsolateSandboxHandle(IsolateSandboxProvider isProvider, IsolateThread isolateThread) {
        this.isProvider = isProvider;
        this.isolateThread = isolateThread;
    }

    public IsolateThread getIsolateThread() {
        return isolateThread;
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws Exception {
        PolyglotFunction function = isProvider.getFunction();
        return isProvider.getGraalvisorAPI().invokeFunction((GuestIsolateThread) isolateThread, function.getEntryPoint(), jsonArguments);
    }

    @Override
    public String toString() {
        return Long.toString(Isolates.getIsolate(isolateThread).rawValue());
    }
}