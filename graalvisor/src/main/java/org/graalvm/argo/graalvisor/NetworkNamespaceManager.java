package org.graalvm.argo.graalvisor;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkNamespaceManager implements Runnable {

    private static final int MAX_NETWORK_NAMESPACES = 256 * 256;
    private final NetworkNamespaceProvider networkNamespaceProvider;

    public NetworkNamespaceManager(NetworkNamespaceProvider networkNamespaceProvider) {
        this.networkNamespaceProvider = networkNamespaceProvider;
    }

    public static void createNamespaces(final NetworkNamespaceProvider networkNamespaceProvider, final int minNetworkNamespaces) {
        final Queue<NetworkNamespace> availableNetworkNamespaces = networkNamespaceProvider.getAvailableNetworkNamespaces();
        final AtomicInteger allNetworkNamespacesCount = networkNamespaceProvider.getNetworkNamespacesCount();
        while (availableNetworkNamespaces.size() < minNetworkNamespaces && allNetworkNamespacesCount.get() < MAX_NETWORK_NAMESPACES) {
            networkNamespaceProvider.createNetworkNamespace();
        }
    }

    public static void deleteNamespaces(final NetworkNamespaceProvider networkNamespaceProvider) {
        final Queue<NetworkNamespace> availableNetworkNamespaces = networkNamespaceProvider.getAvailableNetworkNamespaces();
        while (availableNetworkNamespaces.size() > 800) {
            final NetworkNamespace networkNamespace = availableNetworkNamespaces.poll();
            networkNamespaceProvider.deleteNetworkNamespace(networkNamespace);
        }
    }

    @Override
    public void run() {
        System.out.println("Running periodical check on whether to create or delete network namespaces");
        createNamespaces(networkNamespaceProvider, 40);
        deleteNamespaces(networkNamespaceProvider);
    }
}
