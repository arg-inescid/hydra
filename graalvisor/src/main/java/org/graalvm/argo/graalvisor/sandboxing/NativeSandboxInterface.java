package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native void ginit();

    public static native int createNativeProcessSandbox(int[] childPipe, int[] parentPipe);

    public static native void createNativeIsolateSandbox();

    public static native void createNativeRuntimeSandbox();

    // TODO - createContextSandbox and createContextSnapshotSandbox.

    public static native void createNativeRuntimeSandbox(boolean lazyIsolation);

    public static native int createNetworkNamespace(String jName, int jThirdByte, int jSecondByte);

    public static native int deleteNetworkNamespace(String jName);

    public static native int switchNetworkNamespace(String jName);

    public static native int switchToDefaultNetworkNamespace();

    public static native int enableVeths(String jName, int jThirdByte, int jSecondByte);

    public static native int disableVeths(String jName);

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
