package org.graalvm.argo.lambda_manager.optimizers;

/**
 * This enum describes the type of runtime that the lambda is running.
 */
public enum LambdaExecutionMode {
    // HotSpot JVM with Native Image Agent (single function).
    HOTSPOT_W_AGENT,
    // HotSpot JVM without Native Image Agent (single function).
    HOTSPOT,
    // Graalvisor (multiple functions).
    GRAALVISOR,
    // OpenWhisk VM (single function).
    CUSTOM_JAVA,
    CUSTOM_JAVASCRIPT,
    CUSTOM_PYTHON,

    // Knative (single function with colocation).
    KNATIVE,

    // With profile-guided optimizations enabled.
    GRAALVISOR_PGO,
    // Optimizing with the iprof files.
    GRAALVISOR_PGO_OPTIMIZING,
    // Optimized with PGO.
    GRAALVISOR_PGO_OPTIMIZED,

    // Based on Graalvisor but with reinforced isolation. Runs instrumented function binaries.
    FAASTION,

    // Experimental support for GraalOS.
    GRAALOS;

    public boolean isCustom() {
        return this == LambdaExecutionMode.CUSTOM_JAVA || this == LambdaExecutionMode.CUSTOM_JAVASCRIPT || this == LambdaExecutionMode.CUSTOM_PYTHON;
    }

    public boolean isGraalvisor() {
        return this == LambdaExecutionMode.GRAALVISOR || this == LambdaExecutionMode.GRAALVISOR_PGO || this == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZED || this == LambdaExecutionMode.GRAALVISOR_PGO_OPTIMIZING;
    }

    public boolean isFaastion() {
        return this == LambdaExecutionMode.FAASTION;
    }

    public String getOpenWhiskVMImage() {
        switch (this) {
            case CUSTOM_JAVA:
                return "java-openwhisk";
            case CUSTOM_JAVASCRIPT:
                return "javascript-openwhisk";
            case CUSTOM_PYTHON:
                return "python-openwhisk";
            default:
                throw new IllegalStateException("Cannot get OpenWhisk image for mode: " + this);
        }
    }

    public String getOpenWhiskContainerImage() {
        switch (this) {
            case CUSTOM_JAVA:
                return "openwhisk/java8action:latest";
            case CUSTOM_JAVASCRIPT:
                return "openwhisk/action-nodejs-v12:latest";
            case CUSTOM_PYTHON:
                return "openwhisk/action-python-v3.9:latest";
            default:
                throw new IllegalStateException("Cannot get OpenWhisk image for mode: " + this);
        }
    }
}
