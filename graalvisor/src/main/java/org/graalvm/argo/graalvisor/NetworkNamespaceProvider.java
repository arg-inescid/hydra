package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

import java.util.concurrent.atomic.AtomicInteger;

public class NetworkNamespaceProvider {

	// TODO - move to already existing hooks
	public static void createNetworkNamespace() {
		long start, finish;
		start = System.nanoTime();
		NativeSandboxInterface.createNetworkNamespace();
        finish = System.nanoTime();
		System.out.println(String.format("[thread %s] Created net ns in %s us",	Thread.currentThread().getId(), (finish - start)/1000));
	}

	public static void deleteNetworkNamespace() {
		long start, finish;

		start = System.nanoTime();
		NativeSandboxInterface.deleteNetworkNamespace();
		finish = System.nanoTime();

		System.out.println(String.format("[thread %s] Deleted net ns in %s us",	Thread.currentThread().getId(), (finish - start) / 1000));
	}
}
