package org.graalvm.argo.graalvisor;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

public abstract class Main {

    /**
     * Location where function code will be placed.
     */
    public static String APP_DIR = System.getenv("app_dir");
    public static boolean LAZY_ISOLATION_ENABLED = false;
    public static boolean LAZY_ISOLATION_SUPPORTED = false;

    public static void main(String[] args) throws Exception {
        String lambda_port = System.getenv("lambda_port");
        String lambda_timestamp = System.getenv("lambda_timestamp");
        String lambda_isolation = System.getenv("lambda_isolation"); // `lazy` to enable lazy isolation, `eager` (default) to disable

        if (lambda_timestamp != null) {
            System.out.println(String.format("Graalvisor boot time: %s ms.", (System.currentTimeMillis() - Long.parseLong(lambda_timestamp))));
        }

        if (lambda_port == null) {
            lambda_port = "8080";
        }

        if (APP_DIR == null) {
            APP_DIR = "/tmp/apps/";
        }

        System.out.println(String.format("Graalvisor listening on port %s.", lambda_port));

        if (lambda_isolation != null && lambda_isolation.equals("lazy")) {
            LAZY_ISOLATION_ENABLED = true;
            if (NativeSandboxInterface.isLazyIsolationSupported()) {
                LAZY_ISOLATION_SUPPORTED = true;
            }
            else {
                System.out.println("Warning: graalvisor was compiled without lazy isolation support.");
            }
        }

        // Create the directory where function code will be placed.
        new File(APP_DIR).mkdirs();

        int port = Integer.parseInt(lambda_port);

        if (System.getProperty("java.vm.name").equals("Substrate VM")) {
            // Initialize our native sandbox interface.
            NativeSandboxInterface.ginit();
            boolean lambda_network_isolation = Boolean.parseBoolean(System.getenv("lambda_network_isolation"));
            final Optional<NetworkNamespaceProvider> networkNamespaceProvider;
            if (lambda_network_isolation) {
                int initialNetworkNamespaces;
                try {
                    final String initialNetworkNamespacesStr = System.getenv("lambda_initial_network_namespaces");
                    if (initialNetworkNamespacesStr != null) {
                        initialNetworkNamespaces = Integer.parseInt(initialNetworkNamespacesStr);
                    } else {
                        initialNetworkNamespaces = 64;
                    }
                } catch (final NumberFormatException e) {
                    System.out.println("Could not parse environment variable 'lambda_initial_network_namespaces' to an integer");
                    initialNetworkNamespaces = 64;
                }
                networkNamespaceProvider = Optional.of(new NetworkNamespaceProvider());
                NetworkNamespaceManager.createNamespaces(networkNamespaceProvider.get(), initialNetworkNamespaces);
            } else {
                networkNamespaceProvider = Optional.empty();
            }
            SubstrateVMProxy.networkNamespaceProvider = networkNamespaceProvider;
            new SubstrateVMProxy(port).start();
            networkNamespaceProvider
                .ifPresent(provider -> {
                    final NetworkNamespaceManager networkNamespaceManager = new NetworkNamespaceManager(provider);
                    Executors
                        .newScheduledThreadPool(1)
                        .scheduleAtFixedRate(networkNamespaceManager, 0, 20, TimeUnit.SECONDS);
                });
        } else {
            new HotSpotProxy(port).start();
        }
    }
}
