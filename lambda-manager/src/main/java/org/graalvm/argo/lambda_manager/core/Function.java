package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.lambda.BuildVMM;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Function {

    /** Name of the function. The name of a function is a unique identifier. */
    private final String name;

    /** Function language. */
    private final FunctionLanguage language;

    /** Function entry point (how should we invoke the function). */
    private final String entryPoint;

    /** Memory required to run a function invocation (in MBs). */
    private final long memory;

    /** The runtime where this function should be executed. Accepted values include:
     * - graalvisor (any truffle language, java_lib, and java_native)
     * - <docker image> (e.g., docker.io/openwhisk/action-python-v3.9:latest)
     * */
    private final String runtime;

    /** Function status in the optimization pipeline. */
    private FunctionStatus status;

    /** Flag stating if this function can be co-located with other functions in the same lambda. */
    private final boolean functionIsolation;

    /**
     * There will be only one started Native Image Agent per function, so we need to keep information
     * about PID for that single lambda. We are sending this information to
     * {@link BuildVMM } because during a build, we need to have
     * access to the agent's generated configurations.
     */
    private long lastAgentPID;

    public Function(String name, String language, String entryPoint, String memory, String runtime, boolean functionIsolation) throws Exception {
        this.name = name;
        this.language = FunctionLanguage.fromString(language);
        this.entryPoint = entryPoint;
        this.memory = Long.parseLong(memory);
        this.runtime = runtime;
        if (this.language == FunctionLanguage.NATIVE_JAVA) {
            this.status = FunctionStatus.NOT_BUILT_NOT_CONFIGURED;
        } else {
            this.status = FunctionStatus.READY;
        }
        this.functionIsolation = functionIsolation;
    }

    public String getName() {
        return name;
    }

    public FunctionStatus getStatus() {
        return status;
    }

    public void setStatus(FunctionStatus status) {
        this.status = status;
    }

    public long getLastAgentPID() {
        return lastAgentPID;
    }

    public void setLastAgentPID(long lastAgentPID) {
        this.lastAgentPID = lastAgentPID;
    }

    public FunctionLanguage getLanguage() {
        return language;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public long getMemory() {
        return memory;
    }

    public boolean requiresRegistration() {
        return language != FunctionLanguage.NATIVE_JAVA;
    }

    public Path buildFunctionSourceCodePath() {
        return Paths.get(Environment.CODEBASE, name, name);
    }

    public String getRuntime() {
        return this.runtime;
    }

    public LambdaExecutionMode getLambdaExecutionMode() {
        switch (getStatus()) {
        case NOT_BUILT_NOT_CONFIGURED:
            return LambdaExecutionMode.HOTSPOT_W_AGENT;
        case NOT_BUILT_CONFIGURED:
        case CONFIGURING_OR_BUILDING:
            return LambdaExecutionMode.HOTSPOT;
        case READY:
            if (getLanguage() == FunctionLanguage.NATIVE_JAVA) {
                return LambdaExecutionMode.NATIVE_IMAGE;
            } else if (getRuntime().equals("graalvisor")) {
                return LambdaExecutionMode.GRAALVISOR;
            } else {
                return LambdaExecutionMode.CUSTOM;
            }
        default:
            throw new IllegalStateException("Unexpected value: " + getStatus());
        }
    }

    public boolean isFunctionIsolated() {
        return functionIsolation;
    }
}
