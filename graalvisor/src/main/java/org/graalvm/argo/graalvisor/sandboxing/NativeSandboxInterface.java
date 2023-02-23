package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native int gfork();

    public static native void ginit();
}
