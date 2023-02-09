package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.memory.ElasticMemoryPool;
import org.graalvm.argo.lambda_manager.memory.FixedMemoryPool;
import org.graalvm.argo.lambda_manager.memory.MemoryPool;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;
import io.micronaut.http.client.RxHttpClient;

import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

public class Lambda {

	/** Map of registered functions in this lambda. */
	private final ConcurrentHashMap<String, Function> registeredFunctions;

	/** The lambda id. */
	private long lid;

	/** Number of requests currently being executed. */
	private int openRequestCount;

	/** Number of processed requests since the lambda started. */
	private int closedRequestCount;

	/** Name of the owner of this lambda. */
	private final String username;

	private Timer timer;
	private ConnectionTriplet<String, String, RxHttpClient> connectionTriplet;
	private LambdaExecutionMode executionMode;

	/** Indicates whether this lambda should be used for future requests. */
	private boolean decommissioned;

	/** Memory pool that registers the memory utilized by the lambda. */
	private final MemoryPool memoryPool;

	// TODO - rethink if we should use a function in the constructor.
	public Lambda(Function function) {
		this.registeredFunctions = new ConcurrentHashMap<>();
		this.memoryPool = function.getRuntime().equals(Environment.GRAALVISOR_RUNTIME)
				? new ElasticMemoryPool(Configuration.argumentStorage.getMemoryPool())
				: new FixedMemoryPool(function.getMemory(), function.getMemory());
		if (!function.requiresRegistration()) {
			this.registeredFunctions.put(function.getName(), function);
		}
		this.username = Configuration.coder.decodeUsername(function.getName());
	}

	public long setLambdaID(long lid) {
		return this.lid = lid;
	}

	public long getLambdaID() {
		return this.lid;
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

	public int getOpenRequestCount() {
		return openRequestCount;
	}

	public Timer getTimer() {
		return timer;
	}

	public void resetTimer() {
		Timer oldTimer = timer;
		Timer newTimer = new Timer();
		newTimer.schedule(new DefaultLambdaShutdownHandler(this), Configuration.argumentStorage.getTimeout()
				+ (int) (Configuration.argumentStorage.getTimeout() * Math.random()));
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

	public String getLambdaName() {
		return String.format("lambda_%d_%s", lid, executionMode);
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

	public boolean canRegisterInLambda(Function function) {
	    if (executionMode != LambdaExecutionMode.GRAALVISOR) {
            return (registeredFunctions.isEmpty() || registeredFunctions.contains(function));
        } else {
            if (function.isFunctionIsolated()) {
                return (registeredFunctions.isEmpty() || registeredFunctions.contains(function));
            } else {
                return username.equals(Configuration.coder.decodeUsername(function.getName()));
            }
        }
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

	public MemoryPool getMemoryPool() {
		return memoryPool;
	}

	public String getUsername() {
	    return username;
	}
}
