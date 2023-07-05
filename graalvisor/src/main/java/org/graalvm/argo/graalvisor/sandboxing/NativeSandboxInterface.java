package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native boolean isLazyIsolationSupported();

    public static native void ginit();

    public static native int createNativeProcessSandbox(int[] childPipe, int[] parentPipe, boolean lazyIsolation);

    public static native void enterNativeProcessSandbox();

    public static native void leaveNativeProcessSandbox();

    public static native void destroyNativeProcessSandxbo();

    public static native void createNativeIsolateSandbox(boolean lazyIsolation);

    public static native void enterNativeIsolateSandbox();

    public static native void leaveNativeIsolateSandbox();

    public static native void destroyNativeIsolateSandxbo();

    public static native void createNativeRuntimeSandbox(boolean lazyIsolation);

    public static native void enterNativeRuntimeSandbox();

    public static native void leaveNativeRuntimeSandbox();

    public static native void destroyNativeRuntimeSandxbo();
}
