package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native boolean isLazyIsolationSupported();

    public static native void ginit();

    public static native int createNativeProcessSandbox(int[] childPipe, int[] parentPipe, boolean lazyIsolation);

    public static native void createNativeIsolateSandbox(boolean lazyIsolation);

    public static native void createNativeRuntimeSandbox(boolean lazyIsolation);

    // Methods to access svm-snapshot module (see svm-snapshot.h).
    public static native long svmAttachThread(int svmid);
    public static native String svmEntrypoint(int svmid, long isolateThread);
    public static native void svmDetachThread(int svmid, long isolateThread);
    public static native String svmCheckpoint(int svmid, String functionPath, String args, String metaSnapshotPath, String memSnapshotPath);
    public static native void svmRestore(int svmid, String metaSnapshotPath, String memSnapshotPath);
}
