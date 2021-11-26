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

    /**
     * Arguments passed to the function code when it is launched. Not to be confused with lambda
     * invocation arguments.
     */
    private final String arguments;

    /** Function status in the optimization pipeline. */
    private FunctionStatus status;

    /**
     * There will be only one started Native Image Agent per function, so we need to keep information
     * about PID for that single lambda. We are sending this information to
     * {@link BuildVMM } because during a build, we need to have
     * access to the agent's generated configurations.
     */
    private long lastAgentPID;

    /** Unallocated lambdas. */
    private final ArrayList<Lambda> stoppedLambdas = new ArrayList<>();

    /** Idle lambdas, waiting for requests. */
    private final ArrayList<Lambda> idleLambdas = new ArrayList<>();

    /** Busy lambdas, running requests. */
    private final ArrayList<Lambda> runningLambdas = new ArrayList<>();

    /** Number of Lambdas that are not receiving requests. */
    private int decommissionedLambdas;

    public Function(String name, String language, String entryPoint, String arguments) throws Exception {
        this.name = name;
        this.language = FunctionLanguage.fromString(language);
        this.entryPoint = entryPoint;
        this.arguments = arguments;
        if (isTruffleLanguage()) {
            this.status = FunctionStatus.BUILT;
        } else {
            this.status = FunctionStatus.NOT_BUILT_NOT_CONFIGURED;
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

    public String getArguments() {
        return arguments;
    }

    public long getLastAgentPID() {
        return lastAgentPID;
    }

    public void setLastAgentPID(long lastAgentPID) {
        this.lastAgentPID = lastAgentPID;
    }

    public ArrayList<Lambda> getStoppedLambdas() {
        return stoppedLambdas;
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
        decommissionedLambdas++;
        lambda.setDecommissioned(true);
    }

    public void commissionLambda(Lambda lambda) {
        decommissionedLambdas--;
        lambda.setDecommissioned(false);
    }

    public int getTotalNumberLambdas() {
        return stoppedLambdas.size() + idleLambdas.size() + runningLambdas.size();
    }

    public FunctionLanguage getLanguage() {
        return language;
    }

    public String getEntryPoint() {
        return entryPoint;
    }


    public boolean isTruffleLanguage() {
        return language != FunctionLanguage.JAVA;
    }

    public Path buildFunctionSourceCodePath() {
        return Paths.get(Environment.CODEBASE, name, name);
    }
}
