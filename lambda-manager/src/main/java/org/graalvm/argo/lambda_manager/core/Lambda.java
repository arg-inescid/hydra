package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.memory.FixedMemoryPool;
import org.graalvm.argo.lambda_manager.memory.MemoryPool;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;

import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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
	private String username;

	private Timer timer;
	private LambdaConnection connection;
	private final LambdaExecutionMode executionMode;

	/** Indicates whether this lambda should be used for future requests. */
	private boolean decommissioned;

	/** Memory pool that registers the memory utilized by the lambda. */
	private final MemoryPool memoryPool;

	/** Functions that need to be uploaded to this lambda. */
	private Set<Function> requiresFunctionUpload;

	private String customRuntimeId;

	public Lambda(LambdaExecutionMode executionMode) {
	    this.executionMode = executionMode;
		this.registeredFunctions = new ConcurrentHashMap<>();
        this.requiresFunctionUpload = ConcurrentHashMap.newKeySet();
        this.memoryPool = new FixedMemoryPool(Configuration.argumentStorage.getMaxMemory(), Configuration.argumentStorage.getMaxMemory());
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

	public LambdaConnection getConnection() {
		return connection;
	}

	public void setConnection(LambdaConnection connection) {
		this.connection = connection;
	}

	public LambdaExecutionMode getExecutionMode() {
		return executionMode;
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

	private boolean isRegisteredInLambda(Function function) {
		return this.registeredFunctions.containsValue(function);
	}

    private void setRegisteredInLambda(Function function) {
        if (this.username == null) {
            this.username = Configuration.coder.decodeUsername(function.getName());
        }
        this.registeredFunctions.putIfAbsent(function.getName(), function);
    }

    public boolean canRegisterInLambda(Function function) {
        if (username == null) {
            return true;
        }
        if (executionMode == LambdaExecutionMode.GRAALVISOR) {
            if (function.isFunctionIsolated()) {
                return (registeredFunctions.contains(function)) && username.equals(Configuration.coder.decodeUsername(function.getName()));
            } else {
                // In Graalvisor, functions can be collocated only if they come from the same user. Debug mode allows collocating all functions from all users.
                return Configuration.argumentStorage.isDebugMode() || username.equals(Configuration.coder.decodeUsername(function.getName()));
            }
        } else {
            return (registeredFunctions.contains(function)) && username.equals(Configuration.coder.decodeUsername(function.getName()));
        }
    }

    public synchronized boolean tryRegisterInLambda(Function function) {
        if (canRegisterInLambda(function)) {
            // Only allocate memory in a memory pool if the function is collocatable.
            if (!function.canCollocateInvocation() || memoryPool.allocateMemoryLambda(function.getMemory())) {
                // Success, this lambda fits for the function.
                if (!isRegisteredInLambda(function)) {
                    // Instead of registering function inside synchronized block, we set the flag.
                    setRequiresFunctionUpload(function);
                    setRegisteredInLambda(function);
                }
                return true;
            } else {
                Logger.log(Level.INFO, String.format("[function=%s, mode=%s]: Couldn't allocate memory in lambda %d.", function.getName(), executionMode, lid));
            }
        }
        return false;
    }

	public void resetRegisteredInLambda(Function function) {
		this.registeredFunctions.remove(function.getName());
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

    private void setRequiresFunctionUpload(Function function) {
        requiresFunctionUpload.add(function);
    }

    public boolean isFunctionUploadRequired(Function function) {
        return requiresFunctionUpload.remove(function);
    }

    public String getCustomRuntimeId() {
        return customRuntimeId;
    }

    public void setCustomRuntimeId(String customRuntimeId) {
        this.customRuntimeId = customRuntimeId;
    }

    /** This method is only called after a lambda with Agent terminates. */
    public void updateFunctionStatus() {
        // Iterator is used to efficiently obtain the only element of the set.
        Iterator<Function> iterator = registeredFunctions.values().iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException(String.format("HotSpot with Agent lambda %d does not have any functions registered.", lid));
        }
        Function function = iterator.next();
        if (iterator.hasNext()) {
            throw new IllegalStateException(String.format("HotSpot with Agent lambda %d has more than one functions registered.", lid));
        }
        function.setLastAgentPID(lid);
        function.setStatus(FunctionStatus.NOT_BUILT_CONFIGURED);
    }
}
