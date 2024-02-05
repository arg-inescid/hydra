package org.graalvm.argo.lambda_manager.optimizers;

public enum FunctionStatus {
    // The function have not been built to a Native Image nor we have collected Agent configurations.
    NOT_BUILT_NOT_CONFIGURED,
    // The function has not been build but we have collected Agent configurations.
    NOT_BUILT_CONFIGURED,
    // The function is being build or configured.
    CONFIGURING_OR_BUILDING,
    // The function is ready to be deployed.
    READY,
    //
    PGO_BUILDING,
    PGO_READY,
    PGO_PROFILING_DONE,
    PGO_OPTIMIZED_BUILDING,
    PGO_OPTIMIZED_READY
}
