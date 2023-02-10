package org.graalvm.argo.lambda_manager.optimizers;

/**
 * This enum describes the type of runtime that the lambda is running.
 */
public enum LambdaExecutionMode {
	// HotSpot JVM with Native Image Agent (one function only).
    HOTSPOT_W_AGENT,
    // HotSpot JVM without Native Image Agent (one function only).
    HOTSPOT,
    // GraalVisor (multiple functions).
    GRAALVISOR,
    // Container VM (single function).
    CUSTOM
}
