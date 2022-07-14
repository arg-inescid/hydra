package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

public class Lambda {

    /** The Function that this Lambda is executing. */
    private final Function function; // TODO - remove, we should no longer have a single function.
    private final ConcurrentHashMap<String, Function> registeredFunctions; // TODO - we need to have multiple functions here

    /** The process that is hosting the lambda execution. */
    private ProcessBuilder process;

    /** Number of requests currently being executed. */
    private int openRequestCount;

    /** Number of processed requests since the lambda started. */
    private int closedRequestCount;

    private Timer timer;
    private ConnectionTriplet<String, String, RxHttpClient> connectionTriplet;
    private LambdaExecutionMode executionMode;

    /** Indicates whether this lambda should be used for future requests. */
    private boolean decommissioned;

    public Lambda(Function function) {
        this.function = function;
        this.registeredFunctions = new ConcurrentHashMap<>();
        if (!function.requiresRegistration()) {
            this.registeredFunctions.put(function.getName(), function);
        }
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
        newTimer.schedule(new DefaultLambdaShutdownHandler(this, function),
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

    public String getLambdaPath() throws Exception {
        switch (executionMode) {
        case HOTSPOT:
            return String.format("codebase/%s/pid_%d_hotspot", function.getName(), process.pid());
        case HOTSPOT_W_AGENT:
            return String.format("codebase/%s/pid_%d_hotspot_w_agent", function.getName(), process.pid());
        case NATIVE_IMAGE:
        case GRAALVISOR:
            return String.format("codebase/%s/pid_%d_vmm", function.getName(), process.pid());
        case CUSTOM:
            return String.format("codebase/%s/pid_%d_cruntime", function.getName(), process.pid());
        default:
            throw new Exception("Uknown lambda execution mode: " + executionMode);
        }
    }

    public boolean isDecommissioned() {
        return decommissioned;
    }

    public void setDecommissioned(boolean decommissioned) {
        this.decommissioned = decommissioned;
    }

    public boolean isRegisteredInLambda(Function function) {
        return this.registeredFunctions.containsValue(function);
    }

    public void setRegisteredInLambda(Function function) {
        this.registeredFunctions.putIfAbsent(function.getName(), function);
    }

    public void resetRegisteredInLambda(Function function) {
        if (function.requiresRegistration()) {
            this.registeredFunctions.remove(function.getName());
        }
    }

    public void resetRegisteredInLambda() {
        for (Function function : registeredFunctions.values()) {
            resetRegisteredInLambda(function);
        }
    }
}
