package org.graalvm.argo.graalvisor.sandboxing;

public class NetworkNamespaceHandle {

	private final String name;

	public NetworkNamespaceHandle(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void switchToNetworkNamespace() {
		NativeSandboxInterface.switchNetworkNamespace(name);
	}

	public void switchToDefaultNetworkNamespace() {
		NativeSandboxInterface.switchToDefaultNetworkNamespace();
	}
}
