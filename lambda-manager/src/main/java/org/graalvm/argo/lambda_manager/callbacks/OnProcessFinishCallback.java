package org.graalvm.argo.lambda_manager.callbacks;

public interface OnProcessFinishCallback {
    void finish(int exitCode);
}
