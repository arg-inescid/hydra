package com.lambda_manager.collectors.meta_info;

import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import java.util.Timer;

// TODO: Make this interface.
public class Lambda {

    // TODO: This should be invocation ID (PID) instead of static ID.
    // TODO: Change lambda logs hierarchy to be the same as the one on codebase.
    // TODO: Change logs format to be pid-execution_mode.log in both cases (codebase + lambda logs).
    private int id;
    private int openRequestCount;
    private String args;
    private Timer timer;
    private boolean updated = false;
    private ConnectionTriplet<String, String, RxHttpClient> connectionTriplet;
    private LambdaExecutionMode executionMode; 

    public Lambda(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getOpenRequestCount() {
        return openRequestCount;
    }

    public void setOpenRequestCount(int openRequestCount) {
        this.openRequestCount = openRequestCount;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public ConnectionTriplet<String, String, RxHttpClient> getConnectionTriplet() {
        return connectionTriplet;
    }

    public void setConnectionTriplet(ConnectionTriplet<String, String, RxHttpClient> connectionTriplet) {
        this.connectionTriplet = connectionTriplet;
    }

    public void shouldUpdateID(Function function) {
        if (function.isUpdateID() && !updated) {
            id = function.getId();
            updated = true;
        }
    }

	public LambdaExecutionMode getExecutionMode() {
		return executionMode;
	}

	public void setExecutionMode(LambdaExecutionMode executionMode) {
		this.executionMode = executionMode;
	}
}
