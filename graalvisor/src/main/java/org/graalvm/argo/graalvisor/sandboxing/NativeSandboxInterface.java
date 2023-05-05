package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native int gfork();

    public static native void ginit();

    public static native void createNetworkNamespace(String jName, int jNumber);

    public static native void deleteNetworkNamespace(String jName);
}
