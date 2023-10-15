package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkNamespaceProvider {

	private final Queue<NetworkNamespace> availableNetworkNamespaces;
	private final AtomicInteger count;

	public NetworkNamespaceProvider() {
		this.availableNetworkNamespaces = new ArrayBlockingQueue<>(Integer.MAX_VALUE);
		this.count = new AtomicInteger(0);
	}

	public void createNetworkNamespace() {
		long start, finish;
		start = System.nanoTime();
		final NetworkNamespace networkNamespace = new NetworkNamespace(count.incrementAndGet());
		NativeSandboxInterface.createNetworkNamespace(
			networkNamespace.getName(),
			networkNamespace.getId() % 256,
			networkNamespace.getId() / 256);
		availableNetworkNamespaces.add(networkNamespace);
		finish = System.nanoTime();
		System.out.println(String.format(
			"[thread %s] New network namespace %s in %s us",
			Thread.currentThread().getId(),
			networkNamespace.getName(),
			(finish - start)/1000));
	}

	public void deleteNetworkNamespace(final NetworkNamespace networkNamespace) {
		long start, finish;
		start = System.nanoTime();
		NativeSandboxInterface.deleteNetworkNamespace(networkNamespace.getName());
		count.decrementAndGet();
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
		} while (networkNamespace == null);
		return networkNamespace;
	}

	public void freeNetworkNamespace(final NetworkNamespace networkNamespace) {
		long start, finish;
		start = System.nanoTime();
		NativeSandboxInterface.disableVeths(networkNamespace.getName());
		NativeSandboxInterface.enableVeths(networkNamespace.getName(), networkNamespace.getThirdByte(), networkNamespace.getFourthByte());
		availableNetworkNamespaces.add(networkNamespace);
		finish = System.nanoTime();
		System.out.println(String.format(
			"[thread %s] Freed network namespace %s in %s us",
			Thread.currentThread().getId(),
			networkNamespace.getName(),
			(finish - start) / 1000));
	}

	public Queue<NetworkNamespace> getAvailableNetworkNamespaces() {
		return availableNetworkNamespaces;
	}

	public AtomicInteger getNetworkNamespacesCount() {
		return count;
	}

	public void deleteAllNetworkNamespaces() {
		// TODO: shutdown hook is not working in native image
		availableNetworkNamespaces
			.forEach(this::deleteNetworkNamespace);
	}
}
