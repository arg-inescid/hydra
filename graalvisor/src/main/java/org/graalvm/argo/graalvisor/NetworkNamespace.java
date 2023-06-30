package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

public class NetworkNamespace {

	private final String name;
	private final int ipDigit;

	public NetworkNamespace(final String name, final int ipDigit) {
		this.name = name;
		this.ipDigit = ipDigit;
	}

	public String getName() {
		return name;
	}

	public int getIpDigit() {
		return ipDigit;
	}


	public void switchToNetworkNamespace() {
		NativeSandboxInterface.switchNetworkNamespace(name);
	}

	public void switchToDefaultNetworkNamespace() {
		NativeSandboxInterface.switchToDefaultNetworkNamespace();
	}
}
