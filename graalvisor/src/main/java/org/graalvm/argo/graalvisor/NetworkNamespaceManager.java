package org.graalvm.argo.graalvisor;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkNamespaceManager implements Runnable {

    private static final int MAX_NETWORK_NAMESPACES = 65280; // 255 * 256

    private final NetworkNamespaceProvider networkNamespaceProvider;

    public NetworkNamespaceManager(NetworkNamespaceProvider networkNamespaceProvider) {
        this.networkNamespaceProvider = networkNamespaceProvider;
    }

    @Override
    public void run() {
        System.out.println("Running periodical check on whether to create or delete network namespaces");
        final Queue<NetworkNamespace> availableNetworkNamespaces = networkNamespaceProvider.getAvailableNetworkNamespaces();
        final AtomicLong allNetworkNamespacesCount = networkNamespaceProvider.getNetworkNamespacesCount();
        if (availableNetworkNamespaces.size() < 100 && allNetworkNamespacesCount.get() < MAX_NETWORK_NAMESPACES) {
            while (availableNetworkNamespaces.size() < 100) {
                networkNamespaceProvider.createNetworkNamespace();
            }
        } else if (availableNetworkNamespaces.size() > 300) {
            while (availableNetworkNamespaces.size() > 300) {
                final NetworkNamespace networkNamespace = availableNetworkNamespaces.poll();
                networkNamespaceProvider.deleteNetworkNamespace(networkNamespace);
            }
        }

    }
}
