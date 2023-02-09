package org.graalvm.argo.graalvisor.sandboxing;

import static org.graalvm.argo.graalvisor.utils.IsolateUtils.copyString;
import static org.graalvm.argo.graalvisor.utils.IsolateUtils.retrieveString;

import org.graalvm.argo.graalvisor.RuntimeProxy;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import org.graalvm.argo.graalvisor.base.PolyglotLanguage;
import org.graalvm.argo.graalvisor.utils.IsolateUtils;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.Isolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.function.CEntryPoint;

import com.oracle.svm.graalvisor.types.GuestIsolateThread;

public class IntraProcessSandboxHandle extends SandboxHandle implements Comparable<IntraProcessSandboxHandle> {

    private IsolateThread isolateThread;

    public IntraProcessSandboxHandle(SandboxProvider sprovider, IsolateThread isolateThread) {
        super(sprovider);
        this.isolateThread = isolateThread;
    }

    private Isolate getIsolate() {
        return Isolates.getIsolate(isolateThread);
    }

    public IsolateThread getIsolateThread() {
        return isolateThread;
    }

    @Override
    public int compareTo(IntraProcessSandboxHandle o) {
        return (this.getIsolate().rawValue() == o.getIsolate().rawValue()) ? 0 : 1;
    }

    @CEntryPoint
    public static ObjectHandle invokeFunction(@CEntryPoint.IsolateThreadContext IsolateThread processContext, IsolateThread defaultContext,
                    ObjectHandle functionHandle, ObjectHandle argumentHandle) throws Exception {
        String functionName = retrieveString(functionHandle);
        String argumentString = retrieveString(argumentHandle);
        String resultString = RuntimeProxy.LANGUAGE_ENGINE.invoke(functionName, argumentString);
        return IsolateUtils.copyString(defaultContext, resultString);
    }

    @Override
    public String invokeSandbox(String jsonArguments) throws Exception {
        PolyglotFunction function = sprovider.getFunction();

        if (function.getLanguage().equals(PolyglotLanguage.JAVA)) {
            IsolateSandboxProvider isprovider = (IsolateSandboxProvider) sprovider;
            // TODO - why do we even have a guest isolate thread?
            GuestIsolateThread guestIsolateThread = (GuestIsolateThread) isolateThread;
            return isprovider.getGraalvisorAPI().invokeFunction(guestIsolateThread, function.getEntryPoint(), jsonArguments);
        } else {
            return retrieveString(invokeFunction(isolateThread, CurrentIsolate.getCurrentThread(), copyString(isolateThread, function.getName()), copyString(isolateThread, jsonArguments)));
        }
    }

    @Override
    public String toString() {
        return Long.toString(getIsolate().rawValue());
    }
}