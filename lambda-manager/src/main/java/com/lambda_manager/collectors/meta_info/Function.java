package com.lambda_manager.collectors.meta_info;

import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.processes.ProcessBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class Function {

    private final String name;
    private FunctionStatus status;
    private String arguments;
    /**
     * There will be only one started agent per function, so we need to keep information about PID for that single lambda.
     * We are sending this information to {@link com.lambda_manager.processes.lambda.BuildVMM } because during a build,
     * we need to have access to the agent's generated configurations.
     */
    private long lastAgentPID;

    /** Unallocated lambdas. */
    private final ArrayList<Lambda> stoppedLambdas = new ArrayList<>();

    /** Idle lambdas, waiting for requests. */
    private final ArrayList<Lambda> idleLambdas = new ArrayList<>();

    /** Busy lambdas, running requests. */
    private final ArrayList<Lambda> runningLambdas = new ArrayList<>();

    /** Active processes by PID. */
    private final HashMap<Long, ProcessBuilder> activeProcesses = new HashMap<>();

    /** Number of Lambdas that are not receiving requests. */
    private int decommissedLambdas;

    public Function(String name) {
        this.name = name;
        this.status = FunctionStatus.NOT_BUILT_NOT_CONFIGURED;
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

    public void setArguments(String arguments) {
        this.arguments = arguments;
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

    public void addNewProcess(Long pid, ProcessBuilder processBuilder) {
        activeProcesses.put(pid, processBuilder);
    }

    public ProcessBuilder removeProcess(Long pid) {
        return activeProcesses.remove(pid);
    }

	public int getNumberDecommissedLambdas() {
		return decommissedLambdas;
	}

	public void decommissionLambda(Lambda lambda) {
		decommissedLambdas++;
		lambda.setDecomissioned(true);
	}

	public void commissionLambda(Lambda lambda) {
		decommissedLambdas--;
		lambda.setDecomissioned(false);
	}

	public int getTotalNumberLambdas() {
		return stoppedLambdas.size() + idleLambdas.size() + runningLambdas.size();
	}
}
