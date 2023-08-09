package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native int gfork();

    public static native void ginit();

    public static native void createNetworkNamespace(String jName, int jThirdByte, int jFourthByte);

    public static native void deleteNetworkNamespace(String jName);

    public static native void switchNetworkNamespace(String jName);

    public static native void switchToDefaultNetworkNamespace();

    public static native void enableVeths(String jName);
    public static native void disableVeths(String jName);
}
