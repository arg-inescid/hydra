package org.graalvm.argo.lambda_manager.core;

// TODO - we should get rid of this status. This should be state stored in the lambda. Most likely a map.
public enum LambdaTruffleStatus {
    NEED_REGISTRATION,
    READY_FOR_EXECUTION
}
