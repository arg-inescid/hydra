package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;

import org.graalvm.argo.graalvisor.base.NativeFunction;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;

import com.oracle.svm.graalvisor.api.GraalVisorAPI;

public class ProcessSandboxProvider extends SandboxProvider {

    /**
     * The process sandbox provider loads the function so that child processes can benefit form
     * COW memory.
     */
    private GraalVisorAPI graalvisorAPI;

    public ProcessSandboxProvider(PolyglotFunction function) {
        super(function);
    }

    public GraalVisorAPI getGraalvisorAPI() {
        return this.graalvisorAPI;
    }

    @Override
    public void loadProvider() throws IOException {
        this.graalvisorAPI = new GraalVisorAPI(((NativeFunction) getFunction()).getPath());
    }

    @Override
    public SandboxHandle createSandbox()  throws IOException {
        return new ProcessSandboxHandle(this);
    }

    @Override
    public void destroySandbox(SandboxHandle shandle) throws IOException {
        ((ProcessSandboxHandle) shandle).destroyHandle();
    }

    @Override
    public void unloadProvider() throws IOException {
        graalvisorAPI.close();
    }

    @Override
    public String getName() {
        return "process";
    }
}
