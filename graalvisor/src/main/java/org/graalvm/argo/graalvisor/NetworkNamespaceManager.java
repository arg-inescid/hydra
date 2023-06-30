package org.graalvm.argo.graalvisor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class NetworkNamespaceManager implements Runnable {

    private final NetworkNamespaceProvider networkNamespaceProvider;

    public NetworkNamespaceManager(NetworkNamespaceProvider networkNamespaceProvider) {
        this.networkNamespaceProvider = networkNamespaceProvider;
    }

    @Override
    public void run() {
        System.out.println("Running periodical check on whether to create or delete network namespaces");
        final Queue<NetworkNamespace> availableNetworkNamespaces = networkNamespaceProvider.getAvailableNetworkNamespaces();
        final int initialSize = availableNetworkNamespaces.size();
        if (availableNetworkNamespaces.size() < 100) {
            for (int i = 0 ; i < 100 - initialSize; i++) {
                networkNamespaceProvider.createNetworkNamespace();
            }
        } else {
            for (int i = 0; i < initialSize - 100; i++) {
                final NetworkNamespace networkNamespace = availableNetworkNamespaces.poll();
                if (networkNamespace != null) {
                    networkNamespaceProvider.deleteNetworkNamespace(networkNamespace);
                }
            }
        }
    }
}
