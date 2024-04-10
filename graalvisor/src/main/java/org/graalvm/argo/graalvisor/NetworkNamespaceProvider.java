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
		start = System.nanoTime();
		int id = count.incrementAndGet();
		String netName = genNetName(id);
		NativeSandboxInterface.createNetworkNamespace(netName, id % 256, id / 256);
		finish = System.nanoTime();
		System.out.println(String.format("[thread %s] New network namespace %s in %s us",
			Thread.currentThread().getId(), netName, (finish - start)/1000));
		return netName;
	}

	public static void deleteNetworkNamespace(String netName) {
		long start, finish;
		start = System.nanoTime();
		NativeSandboxInterface.deleteNetworkNamespace(netName);
		count.decrementAndGet();
		finish = System.nanoTime();
		System.out.println(String.format(
			"[thread %s] Deleted network namespace %s in %s us",
			Thread.currentThread().getId(), netName, (finish - start) / 1000));
	}

	public static void switchToNetworkNamespace(String netName) {
        long start = System.nanoTime();
        NativeSandboxInterface.switchNetworkNamespace(netName);
        long end = System.nanoTime();
        System.out.println("SWITCH NAMESPACE: " + Long.valueOf(end - start).toString());
    }

    public static void switchToDefaultNetworkNamespace() {
        long start = System.nanoTime();
        NativeSandboxInterface.switchToDefaultNetworkNamespace();
        long end = System.nanoTime();
        System.out.println("SWITCH NAMESPACE: " + Long.valueOf(end - start).toString());
    }

	public static void cleanupNetworkNamespaces() {
		for (int i = 1; i <= count.get(); i++) {
			System.out.println(String.format("Deleting namespace %d", i));
			deleteNetworkNamespace(genNetName(i));
		}
	}
}
