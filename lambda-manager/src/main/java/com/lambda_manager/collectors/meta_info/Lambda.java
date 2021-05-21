package com.lambda_manager.collectors.meta_info;

import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import java.util.Timer;

// TODO: Make this interface.
public class Lambda {

    private long pid;
    private int openRequestCount;
    private String parameters;
    private Timer timer;
    private ConnectionTriplet<String, String, RxHttpClient> connectionTriplet;
    private LambdaExecutionMode executionMode;

    public Lambda() {
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long pid() {
        return pid;
    }

    public int getOpenRequestCount() {
        return openRequestCount;
    }

    public void setOpenRequestCount(int openRequestCount) {
        this.openRequestCount = openRequestCount;
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
}
