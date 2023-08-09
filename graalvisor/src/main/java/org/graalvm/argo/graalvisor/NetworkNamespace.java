package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

public class NetworkNamespace {

	private final String name;
	private final int thirdByte;
	private final int fourthByte;

	public NetworkNamespace(final int thirdByte, final int fourthByte) {
		this.name = String.format("netns_%s_%s", thirdByte, fourthByte);
		this.thirdByte = thirdByte;
		this.fourthByte = fourthByte;
	}

	public String getName() {
		return name;
	}

	public int getThirdByte() {
		return thirdByte;
	}

	public int getFourthByte() {
		return fourthByte;
	}

	public void switchToNetworkNamespace() {
		NativeSandboxInterface.switchNetworkNamespace(name);
	}

	public void switchToDefaultNetworkNamespace() {
		NativeSandboxInterface.switchToDefaultNetworkNamespace();
	}
}
