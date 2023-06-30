package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class NetworkNamespaceProvider {

	private static final int MAX_COUNT = 256;

	private int count;
	private final Queue<NetworkNamespace> availableNetworkNamespaces;
	private final Map<Integer, String> busyIpDigits;

	public NetworkNamespaceProvider() {
		this.count = 1;
		this.availableNetworkNamespaces = new LinkedBlockingDeque<>();
		this.busyIpDigits = new ConcurrentHashMap<>();
		Executors
			.newSingleThreadScheduledExecutor()
			.scheduleAtFixedRate(
				new NetworkNamespaceManager(this),
				0,
				5,
				TimeUnit.MILLISECONDS);
	}

	public NetworkNamespace createNetworkNamespace() {
		long start, finish;
		start = System.nanoTime();
		synchronized (this) {
			if ((count % MAX_COUNT) == MAX_COUNT - 1) {
				if (count == Integer.MAX_VALUE || count == Integer.MAX_VALUE - 1) {
					count = 1;
				} else {
					count += 2;
				}
			} else {
				if (count == Integer.MAX_VALUE || count == Integer.MAX_VALUE - 1) {
					count = 1;
				} else {
					count++;
				}
			}
		}
		final int ipDigit = (count % MAX_COUNT);
		final String name = String.format("netns%s", ipDigit);
		while (true) {
			final String inserted = busyIpDigits.putIfAbsent(ipDigit, name);
			if (inserted != null) {
				break;
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				throw new RuntimeException("Internal server error.");
			}
		}
		NativeSandboxInterface.createNetworkNamespace(name, ipDigit);
		final NetworkNamespace networkNamespace = new NetworkNamespace(name, ipDigit);
		availableNetworkNamespaces.add(networkNamespace);
		finish = System.nanoTime();
		System.out.println(String.format(
			"[thread %s] New network namespace %s in %s us",
			Thread.currentThread().getId(),
			networkNamespace.getName(),
			(finish - start)/1000));
		return networkNamespace;
	}

	public void deleteNetworkNamespace(final NetworkNamespace networkNamespace) {
		long start, finish;
		start = System.nanoTime();
		NativeSandboxInterface.deleteNetworkNamespace(networkNamespace.getName());
		finish = System.nanoTime();
		System.out.println(String.format(
			"[thread %s] Deleted network namespace %s in %s us",
			Thread.currentThread().getId(),
			networkNamespace.getName(),
			(finish - start) / 1000));
	}

	public NetworkNamespace getAvailableNetworkNamespace() {
		NetworkNamespace networkNamespace;
		do {
			networkNamespace = availableNetworkNamespaces.poll();
		}
		while (networkNamespace == null);
		return networkNamespace;
	}

	public void freeNetworkNamespace(final NetworkNamespace networkNamespace) {
		busyIpDigits.remove(networkNamespace.getIpDigit());
		availableNetworkNamespaces.add(networkNamespace);
	}

	public Queue<NetworkNamespace> getAvailableNetworkNamespaces() {
		return availableNetworkNamespaces;
	}
}
