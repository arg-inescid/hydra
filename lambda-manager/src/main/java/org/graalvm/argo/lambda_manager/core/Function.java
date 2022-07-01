package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.processes.lambda.BuildVMM;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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

    /**
     * There will be only one started Native Image Agent per function, so we need to keep information
     * about PID for that single lambda. We are sending this information to
     * {@link BuildVMM } because during a build, we need to have
     * access to the agent's generated configurations.
     */
    private long lastAgentPID;

    // TODO - functions should not keep these lists anymore. A Lambda will have multiple functions.
    /** Idle lambdas, waiting for requests. */
    private final ArrayList<Lambda> idleLambdas = new ArrayList<>();

    /** Busy lambdas, running requests. */
    private final ArrayList<Lambda> runningLambdas = new ArrayList<>();

    /** Number of Lambdas that are not receiving requests. */
    private int decommissionedLambdas;

    public Function(String name, String language, String entryPoint, String memory, String runtime) throws Exception {
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

    public ArrayList<Lambda> getIdleLambdas() {
        return idleLambdas;
    }

    public ArrayList<Lambda> getRunningLambdas() {
        return runningLambdas;
    }

    public int getNumberDecommissionedLambdas() {
        return decommissionedLambdas;
    }

    public void decommissionLambda(Lambda lambda) {
        if (!lambda.isDecommissioned()) {
            decommissionedLambdas++;
            lambda.setDecommissioned(true);
        }
    }

    public void commissionLambda(Lambda lambda) {
        if (lambda.isDecommissioned()) {
            decommissionedLambdas--;
            lambda.setDecommissioned(false);
        }
    }

    public int getTotalNumberLambdas() {
        return idleLambdas.size() + runningLambdas.size();
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
}
