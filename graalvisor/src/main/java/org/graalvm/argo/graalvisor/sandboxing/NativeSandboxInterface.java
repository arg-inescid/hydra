package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native void ginit();

    public static native int createNativeProcessSandbox(int[] childPipe, int[] parentPipe);

    public static native void createMainCgroup();
    public static native void deleteMainCgroup();

    public static native void createFunctionCgroup(String isolateId);
    public static native void deleteFunctionCgroup(String isolateId);

    public static native void setCgroupWeight(String isolateId, int quota);

    public static native void enterNativeProcessSandbox();

    public static native void leaveNativeProcessSandbox();

    public static native void destroyNativeProcessSandxbo();

    public static native void createNativeIsolateSandbox();

    public static native void enterNativeIsolateSandbox();

    public static native void leaveNativeIsolateSandbox();

    public static native void destroyNativeIsolateSandxbo();

    public static native void createNativeRuntimeSandbox();

    public static native void enterNativeRuntimeSandbox();

    public static native void leaveNativeRuntimeSandbox();

    public static native void destroyNativeRuntimeSandxbo();
}
