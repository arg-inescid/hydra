package org.graalvm.argo.graalvisor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class NetworkNamespaceManager implements Runnable {

    private static final int MAX_NAMESPACES = 255;

    private final NetworkNamespaceProvider networkNamespaceProvider;

    public NetworkNamespaceManager(NetworkNamespaceProvider networkNamespaceProvider) {
        this.networkNamespaceProvider = networkNamespaceProvider;
    }

    @Override
    public void run() {
        System.out.println("Running periodical check on whether to create or delete network namespaces");
        final Queue<NetworkNamespace> availableNetworkNamespaces = networkNamespaceProvider.getAvailableNetworkNamespaces();
        final AtomicLong allNetworkNamespacesCount = networkNamespaceProvider.getNetworkNamespacesCount();
        while (availableNetworkNamespaces.size() < 50) {
            if (allNetworkNamespacesCount.get() == MAX_NAMESPACES) {
                break;
            } else {
                networkNamespaceProvider.createNetworkNamespace();
            }
        }
        // TODO: add condition to remove network namespaces
    }
}
