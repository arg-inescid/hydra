package org.graalvm.argo.lambda_manager.optimizers;

// TODO - we need another mode, POLYGLOT
public enum LambdaExecutionMode {
    HOTSPOT_W_AGENT,
    HOTSPOT,
    NATIVE_IMAGE
}
