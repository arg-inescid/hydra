package org.graalvm.argo.graalvisor;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkNamespaceManager implements Runnable {

    private final NetworkNamespaceProvider networkNamespaceProvider;

    public NetworkNamespaceManager(NetworkNamespaceProvider networkNamespaceProvider) {
        this.networkNamespaceProvider = networkNamespaceProvider;
    }

    @Override
    public void run() {
        System.out.println("Running periodical check on whether to create or delete network namespaces");
        final Queue<NetworkNamespace> availableNetworkNamespaces = networkNamespaceProvider.getAvailableNetworkNamespaces();
        final AtomicLong allNetworkNamespacesCount = networkNamespaceProvider.getNetworkNamespacesCount();
        if (availableNetworkNamespaces.size() < 100) {
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
