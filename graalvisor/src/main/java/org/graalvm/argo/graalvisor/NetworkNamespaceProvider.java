package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkNamespaceProvider {

	private static final int MAX_COUNT_FOURTH_BYTE = 255;
	private static final int MAX_COUNT_THIRD_BYTE = 256;

	private int thirdByte;
	private int fourthByte;
	private final Queue<NetworkNamespace> availableNetworkNamespaces;
	private final AtomicLong count;
	private ScheduledExecutorService scheduledExecutor;

	public NetworkNamespaceProvider() {
		this.thirdByte = 0;
		this.fourthByte = 0;
		this.availableNetworkNamespaces = new ArrayBlockingQueue<>(Integer.MAX_VALUE);
		this.count = new AtomicLong(0);
		this.scheduledExecutor = null;
	}

	public void startScheduler() {
		if (scheduledExecutor == null) {
			this.scheduledExecutor = Executors
					.newSingleThreadScheduledExecutor();
			scheduledExecutor
					.scheduleAtFixedRate(
							new NetworkNamespaceManager(this),
							0,
							5,
							TimeUnit.SECONDS);
		}
	}

	public void createNetworkNamespace() {
		long start, finish;
		start = System.nanoTime();
		final int newThirdByte;
		final int newFourthByte;
		synchronized (this) {
			if ((fourthByte + 1) % MAX_COUNT_FOURTH_BYTE == 0) {
				if ((thirdByte + 1) % MAX_COUNT_THIRD_BYTE == 0) {
					thirdByte = 0;
					fourthByte = 1;
				} else {
					thirdByte++;
					fourthByte = 1;
				}
			} else {
				fourthByte++;
			}
			newThirdByte = thirdByte;
			newFourthByte = fourthByte;
		}
		final NetworkNamespace networkNamespace = new NetworkNamespace(newThirdByte, newFourthByte);
		NativeSandboxInterface.createNetworkNamespace(
			networkNamespace.getName(),
			networkNamespace.getThirdByte(),
			networkNamespace.getFourthByte());
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
		}
		while (networkNamespace == null);
		return networkNamespace;
	}

	public void freeNetworkNamespace(final NetworkNamespace networkNamespace) {
		long start, finish;
		start = System.nanoTime();
		NativeSandboxInterface.disableVeths(networkNamespace.getName());
		NativeSandboxInterface.enableVeths(networkNamespace.getName());
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

	public AtomicLong getNetworkNamespacesCount() {
		return count;
	}

	public void deleteAllNetworkNamespaces() {
		scheduledExecutor
			.shutdown();
		availableNetworkNamespaces
			.forEach(this::deleteNetworkNamespace);
	}
}
