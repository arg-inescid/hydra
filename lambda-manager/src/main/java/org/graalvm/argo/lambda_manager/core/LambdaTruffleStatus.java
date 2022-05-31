package org.graalvm.argo.lambda_manager.core;

// TODO - I don't think we need the deregister method. If a function is deregistered, it goes immediately back to need_registration.
public enum LambdaTruffleStatus {
    NOT_TRUFFLE_LANG,
    NEED_REGISTRATION,
    READY_FOR_EXECUTION,
    DEREGISTER
}
