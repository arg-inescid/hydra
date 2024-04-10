package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

import java.util.concurrent.atomic.AtomicInteger;

public class NetworkNamespaceProvider {

	private static final AtomicInteger count = new AtomicInteger(0);

	private static String genNetName(int id) {
		return String.format("netns_%s", id);
	}

	public static String createNetworkNamespace() {
		long start, finish;
		int id = count.incrementAndGet();
		String netName = genNetName(id);

		start = System.nanoTime();
		NativeSandboxInterface.createNetworkNamespace(netName, id % 256, id / 256);
        finish = System.nanoTime();

		System.out.println(String.format("[thread %s] Created net ns %s in %s us",
			Thread.currentThread().getId(), netName, (finish - start)/1000));
		return netName;
	}

	public static void deleteNetworkNamespace(String netName) {
		long start, finish;

		start = System.nanoTime();
		NativeSandboxInterface.deleteNetworkNamespace(netName);
		count.decrementAndGet(); // TODO - this is not correct. We may decrement and reuse ids!
		finish = System.nanoTime();

		System.out.println(String.format("[thread %s] Deleted net ns %s in %s us",
			Thread.currentThread().getId(), netName, (finish - start) / 1000));
	}

	public static void cleanupNetworkNamespaces() {
		for (int i = 1; i <= count.get(); i++) {
			deleteNetworkNamespace(genNetName(i));
		}
	}
}
