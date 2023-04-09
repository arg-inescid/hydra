package org.graalvm.argo.graalvisor.sandboxing;

public class NativeSandboxInterface {

    public static native int gfork(int[] childPipe, int[] parentPipe);

    public static native void ginit();
}
