package org.graalvm.argo.graalvisor.sandboxing;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkNamespaceProvider {

	private static final int MAX_COUNT = 500000;

	private final AtomicInteger count;

	public NetworkNamespaceProvider() {
		this.count = new AtomicInteger();
		this.count.set(0); // first value will be 1
	}

	public NetworkNamespaceHandle createNetworkNamespace() {
		int value = count.updateAndGet(val -> (val + 1) % MAX_COUNT);
		final String name = String.format("netns%s", value);
		NativeSandboxInterface.createNetworkNamespace(name, value);
		return new NetworkNamespaceHandle(name);
	}

	public void deleteNetworkNamespace(final NetworkNamespaceHandle networkNamespaceHandle) {
		NativeSandboxInterface.deleteNetworkNamespace(networkNamespaceHandle.getName());
	}

}
