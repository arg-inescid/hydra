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
	private final Map<String, NetworkNamespace> busyNetworkNamespaces;
	private final AtomicLong count;
	private final ScheduledExecutorService scheduledExecutor;

	public NetworkNamespaceProvider() {
		this.thirdByte = 0;
		this.fourthByte = 0;
		this.availableNetworkNamespaces = new ArrayBlockingQueue<>(256);
		this.busyNetworkNamespaces = new ConcurrentHashMap<>();
		this.count = new AtomicLong(0);
		this.scheduledExecutor = Executors
			.newSingleThreadScheduledExecutor();
		scheduledExecutor
			.scheduleAtFixedRate(
				new NetworkNamespaceManager(this),
				0,
				5,
				TimeUnit.SECONDS);
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
		while (true) {
			final String key = String.format(
				"%s_%s",
				networkNamespace.getThirdByte(),
				networkNamespace.getFourthByte());
			final NetworkNamespace inserted = busyNetworkNamespaces.putIfAbsent(key, networkNamespace);
			if (inserted != null) {
				break;
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				throw new RuntimeException("Internal server error.");
			}
		}
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
		final String key = String.format(
			"%s_%s",
			networkNamespace.getThirdByte(),
			networkNamespace.getFourthByte());
		busyNetworkNamespaces.remove(key);
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
		busyNetworkNamespaces
			.forEach((ipDigit, networkNamespace) -> this.deleteNetworkNamespace(networkNamespace));
	}
}
