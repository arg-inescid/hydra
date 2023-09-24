package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native void ginit();
    public static native int createNativeProcessSandbox(int[] childPipe, int[] parentPipe);
    public static native int getThreadId();

    public static native void createMainCgroup();
    public static native void deleteMainCgroup();

    public static native void createCgroup(String cgroupId);
    public static native void deleteCgroup(String cgroupId);

    public static native void insertThreadInCgroup(String cgroupId, String threadId);
    public static native void removeThreadFromCgroup(String cgroupId);

    public static native void setCgroupQuota(String cgroupId, int quota);

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