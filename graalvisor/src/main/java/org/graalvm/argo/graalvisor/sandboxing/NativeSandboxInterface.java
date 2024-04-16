package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native void initialize();

    public static native void teardown();

    public static native int createNativeProcessSandbox(int[] childPipe, int[] parentPipe);

    public static native void createNativeIsolateSandbox();

    public static native void createNativeRuntimeSandbox();

    // TODO - createContextSandbox and createContextSnapshotSandbox.
    // TODO - we should be able to enable isolation only if a particular environment variable is on.
    // The main reason for this is that vms and containers already provide isolation and we don't know the backend at compile time.

    public static native int createNetworkNamespace();

    public static native int deleteNetworkNamespace();

    // Methods to access svm-snapshot module (see svm-snapshot.h).
    public static native long svmAttachThread(int svmid);
    public static native String svmEntrypoint(int svmid, long isolateThread, String args);
    public static native void svmDetachThread(int svmid, long isolateThread);
    public static native String svmCheckpoint(
        int svmid,
        String functionPath,
        int concurrency,
        int requests,
        String functionArgs,
        String metaSnapshotPath,
        String memSnapshotPath);
    public static native void svmRestore(
        int svmid,
        String functionPath,
        String metaSnapshotPath,
        String memSnapshotPath);
}
