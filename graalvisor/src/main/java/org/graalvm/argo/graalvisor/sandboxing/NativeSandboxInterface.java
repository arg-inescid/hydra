package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native boolean isLazyIsolationSupported();

    public static native void ginit();

    public static native int createNativeProcessSandbox(int[] childPipe, int[] parentPipe, boolean lazyIsolation);

    public static native void createNativeIsolateSandbox(boolean lazyIsolation);

    public static native void createNativeRuntimeSandbox(boolean lazyIsolation);

    // Methods to access svm-snapshot module (see svm-snapshot.h).
    public static native String checkpointSVM(String functionPath, String args, String metaSnapshotPath, String memSnapshotPath);
    public static native long restoreSVM(String metaSnapshotPath, String memSnapshotPath);
}
