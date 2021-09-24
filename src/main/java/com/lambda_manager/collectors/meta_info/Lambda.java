package com.lambda_manager.collectors.meta_info;

import com.lambda_manager.core.Configuration;
import com.lambda_manager.handlers.DefaultLambdaShutdownHandler;
import com.lambda_manager.optimizers.LambdaExecutionMode;
import com.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import com.lambda_manager.processes.ProcessBuilder;

import java.util.Timer;

public class Lambda {

    /** The Function that this Lambda is executing. */
    private final Function function;

    /** The process that is hosting the lambda execution. */
    private ProcessBuilder process;

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

    public Lambda(Function function) {
        this.function = function;
    }

    public Function getFunction() {
        return this.function;
    }

    public ProcessBuilder getProcess() {
        return this.process;
    }

    public void setProcess(ProcessBuilder process) {
        this.process = process;
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

    public void resetTimer() {
        Timer oldTimer = timer;
        Timer newTimer = new Timer();
        newTimer.schedule(new DefaultLambdaShutdownHandler(this),
                Configuration.argumentStorage.getTimeout() +
                (int)(Configuration.argumentStorage.getTimeout() * Math.random()));
        timer = newTimer;
        if (oldTimer != null) {
            oldTimer.cancel();
        }
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
