package org.graalvm.argo.lambda_manager.processes.lambda;

public interface OnProcessFinishCallback {
    void finish(int exitCode);
}
