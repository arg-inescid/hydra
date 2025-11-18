package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.optimizers.ColdStartSlidingWindow;
import org.graalvm.argo.lambda_manager.optimizers.FunctionStatus;
import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.lambda.BuildNativeImagePgo;
import org.graalvm.argo.lambda_manager.processes.lambda.BuildNativeImagePgoOptimized;
import org.graalvm.argo.lambda_manager.processes.lambda.BuildSO;
import org.graalvm.argo.lambda_manager.utils.MinioUtils;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public class Function {

    /** Name of the function. The name of a function is a unique identifier. */
    private final String name;

    /** Function language. */
    private final FunctionLanguage language;

    /** Function entry point (how should we invoke the function). */
    private final String entryPoint;

    /** Memory required to run a function invocation (in MBs). */
    private final long memory;

    /** The runtime where this function should be executed. Accepted values include:
     * - hydra (any truffle language, java)
     * - <docker image> (e.g., docker.io/openwhisk/action-python-v3.9:latest)
     * */
    private final String runtime;

    /** Function status in the optimization pipeline. */
    private FunctionStatus status;

    /** Flag stating if this function can be co-located with other functions in the same lambda. */
    private final boolean functionIsolation;

    /** Flag stating if instances of this function can be co-located in the same lambda. */
    private final boolean invocationCollocation;

    /** Desired sandbox for Hydra runtime. Can only be used with Hydra. */
    private final String gvSandbox;

    /** SVM ID used for sandbox checkpoint/restore for this function. Should be a valid small integer. Can only be used with Hydra. Can be null. */
    private final String svmId;

    /** If the function has the Hydra or HotSpot mode - URL of the function code to be downloaded by Hydra.
     * If the function is Knative - name of the Docker image for this function. */
    private final String functionCode;

    /** Flag stating if this function can be re-built into native image in case of fallback (only for Hydra). */
    private final boolean canRebuild;

    /** Sliding window for determining if this function is worth optimizing. Counts number of cold starts per period. */
    private final ColdStartSlidingWindow window;

    /**
     * There will be only one started Native Image Agent per function, so we need to
     * keep information about PID for that single lambda. We are sending this
     * information to {@link BuildSO } because during a build, we need to have
     * access to the agent's generated configurations.
     */
    private long lastAgentPID;

    public Function(String name, String language, String entryPoint, String memory, String runtime, String functionCode, boolean functionIsolation, boolean invocationCollocation, String gvSandbox, String svmId) throws Exception {
        this.name = name;
        this.language = FunctionLanguage.fromString(language);
        this.entryPoint = entryPoint;
        this.memory = Long.parseLong(memory);
        this.runtime = runtime;
        this.functionCode = functionCode;
        this.canRebuild = runtime.equals(Environment.HYDRA_RUNTIME) && this.isJar(functionCode);
        if (this.canRebuild) {
            this.status = FunctionStatus.NOT_BUILT_NOT_CONFIGURED;
        } else {
            this.status = FunctionStatus.READY;
        }
        this.functionIsolation = functionIsolation;
        this.invocationCollocation = invocationCollocation;
        this.gvSandbox = gvSandbox;
        this.svmId = svmId;
        this.window = new ColdStartSlidingWindow(Environment.AOT_OPTIMIZATION_THRESHOLD, Environment.SLIDING_WINDOW_PERIOD);
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

    public long getLastAgentPID() {
        return lastAgentPID;
    }

    public void setLastAgentPID(long lastAgentPID) {
        this.lastAgentPID = lastAgentPID;
    }

    public FunctionLanguage getLanguage() {
        return language;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public long getMemory() {
        return memory;
    }

    public Path buildFunctionSourceCodePath() {
        if (canRebuild && getLambdaExecutionMode() == LambdaExecutionMode.HYDRA) {
            // The function was uploaded for GV target and its .so is built
            return Paths.get(Environment.CODEBASE, name, "build_so", "lib" + name + ".so");
        } else if (canRebuild && getLambdaExecutionMode() == LambdaExecutionMode.HYDRA_PGO) {
            return Paths.get(Environment.CODEBASE, name, "pgo-enable", name );
        } else if (canRebuild && getLambdaExecutionMode() == LambdaExecutionMode.HYDRA_PGO_OPTIMIZING) {
            return Paths.get(Environment.CODEBASE, name, "pgo-enable-optimizing", name );
        } else if (canRebuild && getLambdaExecutionMode() == LambdaExecutionMode.HYDRA_PGO_OPTIMIZED) {
            return Paths.get(Environment.CODEBASE, name, "pgo-optimized", name );
        }
        return Paths.get(Environment.CODEBASE, name, name);
    }

    public String getRuntime() {
        return this.runtime;
    }

    public LambdaExecutionMode getLambdaExecutionMode() {
        switch (getStatus()) {
            case NOT_BUILT_NOT_CONFIGURED:
                return LambdaExecutionMode.HOTSPOT_W_AGENT;
            case NOT_BUILT_CONFIGURED:
            case CONFIGURING_OR_BUILDING:
                return LambdaExecutionMode.HOTSPOT;
            case READY:
                if (getRuntime().equals(Environment.HYDRA_RUNTIME)) {
                    return LambdaExecutionMode.HYDRA;
                } else if (getRuntime().equals(Environment.GRAALOS_RUNTIME)) {
                    return LambdaExecutionMode.GRAALOS;
                } else if (getRuntime().equals(Environment.KNATIVE_RUNTIME)) {
                    return LambdaExecutionMode.KNATIVE;
                } else {
                    switch (getLanguage()) {
                        case JAVA:
                            return LambdaExecutionMode.CUSTOM_JAVA;
                        case JAVASCRIPT:
                            return LambdaExecutionMode.CUSTOM_JAVASCRIPT;
                        case PYTHON:
                            return LambdaExecutionMode.CUSTOM_PYTHON;
                        default:
                            throw new IllegalStateException("Unexpected language: " + getLanguage());
                    }
                }
            case PGO_BUILDING:
                return LambdaExecutionMode.HYDRA;
            case PGO_READY:
            case PGO_OPTIMIZED_BUILDING:
            case PGO_PROFILING_DONE:
                return LambdaExecutionMode.HYDRA_PGO;
            case PGO_OPTIMIZED_READY:
                return LambdaExecutionMode.HYDRA_PGO_OPTIMIZED;
            default:
                throw new IllegalStateException("Unexpected value: " + getStatus());
        }
    }

    public boolean isFunctionIsolated() {
        return this.functionIsolation || Configuration.argumentStorage.isSnapshotEnabled();
    }

    public boolean canCollocateInvocation() {
        // Lambda execution mode can change from "non-collocatable" to "collocatable"
        // runtime and back throughout the function lifetime as the function might go
        // through the build pipeline and fallback.
        LambdaExecutionMode mode = getLambdaExecutionMode();
        if (mode == LambdaExecutionMode.HOTSPOT_W_AGENT || mode == LambdaExecutionMode.HOTSPOT || mode.isCustom()) {
            return false;
        }
        return !Configuration.argumentStorage.isSnapshotEnabled() && this.invocationCollocation;
    }

    public String getHydraSandbox() {
        return this.gvSandbox;
    }

    public boolean canRebuild() {
        return this.canRebuild;
    }

    private boolean isJar(String functionUrl) {
        return functionUrl != null && functionUrl.endsWith(".jar");
    }

    public boolean snapshotSandbox() {
        return svmId != null && "snapshot".equals(gvSandbox);
    }

    public String getSvmId() {
        return svmId;
    }

    /**
     * Update status when creating a new lambda for this function.
     */
    public synchronized void updateStatus(LambdaExecutionMode targetMode) {
        long currentTimestamp = System.currentTimeMillis();
        window.addColdStart(currentTimestamp);
        switch (targetMode) {
            case HOTSPOT_W_AGENT:
                status = FunctionStatus.CONFIGURING_OR_BUILDING;
                break;
            case HOTSPOT:
                if (status == FunctionStatus.NOT_BUILT_CONFIGURED && window.worthOptimizing(currentTimestamp)) {
                    status = FunctionStatus.CONFIGURING_OR_BUILDING;
                    new BuildSO(this).build().start();
                    Logger.log(Level.INFO, "Starting new .so build for function " + name);
                }
                break;
            case HYDRA:
                if (status == FunctionStatus.READY) {
                    status = FunctionStatus.PGO_BUILDING;
                    new BuildNativeImagePgo(this).build().start();
                    Logger.log(Level.INFO, "Starting new native image with PGO enabled " + name);
                }
                break;
            case HYDRA_PGO:
                MinioUtils minioUtils = new MinioUtils();
                if (status == FunctionStatus.PGO_PROFILING_DONE && minioUtils.containsAnyProfile(name)) {
                    minioUtils.downloadProfiles(name);
                    status = FunctionStatus.PGO_OPTIMIZED_BUILDING;
                    new BuildNativeImagePgoOptimized(this).build().start();
                    Logger.log(Level.INFO, "Starting new native image with PGO optimized " + name);
                }
                break;
            default:
                break;
        }
    }

    public String getFunctionCode() {
        return functionCode;
    }
}
