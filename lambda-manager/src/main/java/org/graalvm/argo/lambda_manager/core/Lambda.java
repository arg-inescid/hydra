package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;

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

    /** Lambda status in the truffle pipeline. */
    private volatile LambdaTruffleStatus truffleStatus;

    private Timer timer;
    private ConnectionTriplet<String, String, RxHttpClient> connectionTriplet;
    private LambdaExecutionMode executionMode;

    /** Indicates whether this lambda should be used for future requests. */
    private boolean decommissioned;

    public Lambda(Function function) {
        this.function = function;
        if (function.isTruffleLanguage()) {
            this.truffleStatus = LambdaTruffleStatus.NEED_REGISTRATION;
        } else {
            this.truffleStatus = LambdaTruffleStatus.NOT_TRUFFLE_LANG;
        }
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

    public boolean isDecommissioned() {
        return decommissioned;
    }

    public void setDecommissioned(boolean decommissioned) {
        this.decommissioned = decommissioned;
    }

    public LambdaTruffleStatus getTruffleStatus() {
        return truffleStatus;
    }

    public void setTruffleStatus(LambdaTruffleStatus truffleStatus) {
        this.truffleStatus = truffleStatus;
    }
}
