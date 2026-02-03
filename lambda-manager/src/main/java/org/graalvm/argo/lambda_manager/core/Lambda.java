package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Lambda {

	/** Map of registered functions in this lambda. */
	private final ConcurrentHashMap<String, Function> registeredFunctions;

	/** The lambda id. */
	private long lid;

	/** Number of requests currently being executed. */
	private final AtomicInteger openRequestCount;

	/** Number of processed requests since the lambda started. */
	private int closedRequestCount;

	/** Name of the owner of this lambda. */
	private String username;

	private long lastUsedTimestamp;

    /** Connection used to send invocation requests and receive responses. */
    private LambdaConnection connection;

    /** Type lambda execution (see LambdaExecutionMode for details). */
	private final LambdaExecutionMode executionMode;

	/** Indicates whether this lambda should be used for future requests. */
	private boolean decommissioned;

	/** Functions that need to be uploaded to this lambda. */
	private final Set<Function> requiresFunctionUpload;

    /** Lock to terminate this lambda at most once (to avoid lambda shutdown handler processing the same lambda multiple times). */
    private final AtomicBoolean terminationLock;

    public Lambda(LambdaExecutionMode executionMode) {
        this.openRequestCount = new AtomicInteger(0);
        this.executionMode = executionMode;
        this.registeredFunctions = new ConcurrentHashMap<>();
        this.requiresFunctionUpload = ConcurrentHashMap.newKeySet();
        this.terminationLock = new AtomicBoolean(false);
    }

	public long setLambdaID(long lid) {
		return this.lid = lid;
	}

	public long getLambdaID() {
		return this.lid;
	}

    public void decOpenRequests() {
        if (openRequestCount.decrementAndGet() == 0) {
            updateLastUsed();
        }
        ++closedRequestCount;
    }

	public void resetClosedRequestCount() {
		closedRequestCount = 0;
	}

	public int getClosedRequestCount() {
		return closedRequestCount;
	}

	public int getOpenRequestCount() {
		return openRequestCount.get();
	}

	public void updateLastUsed() {
		lastUsedTimestamp = System.currentTimeMillis();
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

    // TODO: check for all places (including this method) where we should add new modes
    // to configure "collocatability" and other things.
    public boolean canRegisterInLambda(Function function) {
        if (username == null) {
            return true;
        }

        if (executionMode == LambdaExecutionMode.HYDRA || executionMode == LambdaExecutionMode.GRAALOS) {
            if (function.isFunctionIsolated()) {
                return (registeredFunctions.contains(function)) && username.equals(Configuration.coder.decodeUsername(function.getName()));
            } else {
                // In Hydra, functions can be collocated only if they come from the same user. Debug mode allows collocating all functions from all users.
                return Configuration.argumentStorage.isDebugMode() || username.equals(Configuration.coder.decodeUsername(function.getName()));
            }
        } else {
            return (registeredFunctions.contains(function)) && username.equals(Configuration.coder.decodeUsername(function.getName()));
        }
    }

    public boolean tryRegisterInLambda(Function function) {
        boolean lambdaAvailable = tryBookLambda(function);
        if (lambdaAvailable) {
            // We need the synchronized block to avoid double-registration in collocatable lambdas.
            // The race condition happens because we first test if we can register, and then set registered.
            synchronized (this) {
                if (canRegisterInLambda(function)) {
                    // Success, this lambda fits for the function.
                    if (!isRegisteredInLambda(function)) {
                        // Instead of registering function inside synchronized block, we set the flag.
                        setRequiresFunctionUpload(function);
                        setRegisteredInLambda(function);
                    }
                    return true;
                } else {
                    // Decrement open requests as we cannot use the lambda for this request.
                    decOpenRequests();
                }
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

	public String getUsername() {
	    return username;
	}

    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }

    private void setRequiresFunctionUpload(Function function) {
        requiresFunctionUpload.add(function);
    }

    public boolean isFunctionUploadRequired(Function function) {
        return requiresFunctionUpload.remove(function);
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

    /**
     * Check whether this lambda has not been used.
     */
    public boolean isIntact() {
        return !decommissioned && username == null && requiresFunctionUpload.isEmpty();
    }

    // Booking a lambda primarily matters for non-collocatable modes.
    public boolean tryBookLambda(Function function) {
        // Increment the open request count (replaces incOpenRequests).
        return function.canCollocateInvocation() ? openRequestCount.incrementAndGet() > 0 : openRequestCount.compareAndSet(0, 1);
    }

    public boolean tryAcquireTerminationLock() {
        return terminationLock.compareAndSet(false, true);
    }
}
