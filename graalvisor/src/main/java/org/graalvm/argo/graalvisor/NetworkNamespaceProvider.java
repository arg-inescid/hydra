package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

import java.util.concurrent.atomic.AtomicInteger;

public class NetworkNamespaceProvider {

	private static final boolean[] availavleNetIDs = new boolean[256*256];

	static {
		for (int i = 0; i < availavleNetIDs.length; i++) {
			availavleNetIDs[i] = true;
		}
	}

	private static synchronized boolean acquireNetworkID(int id) {
		if (availavleNetIDs[id]) {
			availavleNetIDs[id] = false;
			return true;
		} else {
			return false;
		}
	}

	private static synchronized int acquireNetworkID() {
		for (int i = 0; i < availavleNetIDs.length; i++) {
			if (availavleNetIDs[i] && acquireNetworkID(i)) {
				return i;
			}
		}
		return -1;
	}

	private static synchronized void releaseNetworkID(int i) {
		availavleNetIDs[i] = true;
	}

	private static String genNetName(int id) {
		return String.format("netns_%s", id);
	}

	private static int getNetFromName(String name) {
		return Integer.parseInt(name.replace("netns_", ""));
	}

	public static String createNetworkNamespace() {
		long start, finish;
		int id = acquireNetworkID();
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
		releaseNetworkID(getNetFromName(netName));
		finish = System.nanoTime();

		System.out.println(String.format("[thread %s] Deleted net ns %s in %s us",
			Thread.currentThread().getId(), netName, (finish - start) / 1000));
	}

	public static void cleanupNetworkNamespaces() {
		for (int i = 0; i < availavleNetIDs.length; i++) {
			if (!availavleNetIDs[i]) {
				deleteNetworkNamespace(genNetName(i));
			}
		}
	}
}
