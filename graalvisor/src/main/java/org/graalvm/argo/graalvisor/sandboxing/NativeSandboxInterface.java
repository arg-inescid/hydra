package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native boolean isLazyIsolationSupported();

    public static native void ginit();

    public static native int createNativeProcessSandbox(int[] childPipe, int[] parentPipe, boolean lazyIsolation);

    public static native void createNativeIsolateSandbox(boolean lazyIsolation);

    public static native void createNativeRuntimeSandbox(boolean lazyIsolation);

    public static native int createNetworkNamespace(String jName, int jThirdByte, int jSecondByte);

    public static native int deleteNetworkNamespace(String jName);

    public static native int switchNetworkNamespace(String jName);

    public static native int switchToDefaultNetworkNamespace();

    public static native int enableVeths(String jName, int jThirdByte, int jSecondByte);

    public static native int disableVeths(String jName);
}
