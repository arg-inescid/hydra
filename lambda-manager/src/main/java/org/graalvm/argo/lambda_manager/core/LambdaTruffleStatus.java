package org.graalvm.argo.lambda_manager.core;

public enum LambdaTruffleStatus {
    NOT_TRUFFLE_LANG,
    NEED_REGISTRATION,
    READY_FOR_EXECUTION,
    DEREGISTER
}
