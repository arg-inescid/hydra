package com.lambda_manager.collectors.meta_info;

import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import java.util.Timer;

public class Lambda {

    private long pid;

    /** Number of requests currently being executed. */
    private int openRequestCount;

    /** Number of processed requests since the lambda started. */
    private int closedRequestCount;

    private String parameters;
    private Timer timer;
    private ConnectionTriplet<String, String, RxHttpClient> connectionTriplet;
    private LambdaExecutionMode executionMode;

    /** Indicates whether or not this lambda should be used for future requests. */
    private boolean decomissioned;

    public Lambda() {
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long pid() {
        return pid;
    }

    public int incOpenRequestCount() {
        return ++openRequestCount;
    }

    public int decOpenRequestCount() {
        return --openRequestCount;
    }

    public int incClosedRequestCount() {
        return ++closedRequestCount;
    }

    public int decClosedRequestCount() {
        return --closedRequestCount;
    }

    public void resetClosedRequestCount() {
        closedRequestCount = 0;
    }

    public int getClosedRequestCount() {
        return closedRequestCount;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
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

    public LambdaExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(LambdaExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

	public boolean isDecomissioned() {
		return decomissioned;
	}

	public void setDecomissioned(boolean decomissioned) {
		this.decomissioned = decomissioned;
	}

}
