package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

public class NetworkNamespace {

    private final int id;
    private final int thirdByte;
    private final int fourthByte;

    public NetworkNamespace(final int id) {
        this.id = id;
        this.thirdByte = id % 256;
        this.fourthByte = id / 256;
    }

    public String getName() {
        return String.format("netns_%s", id);
    }

    public int getId() {
        return id;
    }

    public int getThirdByte() {
        return thirdByte;
    }

    public int getFourthByte() {
        return fourthByte;
    }

    public void switchToNetworkNamespace() {
        long start = System.nanoTime();
        NativeSandboxInterface.switchNetworkNamespace(getName());
        long end = System.nanoTime();
        System.out.println("SWITCH NAMESPACE: " + Long.valueOf(end - start).toString());
    }

    public void switchToDefaultNetworkNamespace() {
        long start = System.nanoTime();
        NativeSandboxInterface.switchToDefaultNetworkNamespace();
        long end = System.nanoTime();
        System.out.println("SWITCH NAMESPACE: " + Long.valueOf(end - start).toString());
    }
}
