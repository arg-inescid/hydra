package org.graalvm.argo.graalvisor;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

import java.io.File;

public abstract class Main {

    /**
     * Location where function code will be placed.
     */
    public static String APP_DIR = System.getenv("app_dir");

    public static void main(String[] args) throws Exception {
        String lambda_port = System.getenv("lambda_port");
        String lambda_timestamp = System.getenv("lambda_timestamp");
        boolean useCgroupCache = Boolean.parseBoolean(System.getenv("use_cgroup_cache"));

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

        // Create the directory where function code will be placed.
        new File(APP_DIR).mkdirs();

        int port = Integer.parseInt(lambda_port);
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());

        if (System.getProperty("java.vm.name").equals("Substrate VM")) {
            // Initialize our native sandbox interface.
            NativeSandboxInterface.ginit();
            NativeSandboxInterface.createMainCgroup();
            new SubstrateVMProxy(port, useCgroupCache).start();
        } else {
            new HotSpotProxy(port).start();
        }
    }

    static class ShutDownHook extends Thread {
        public void run() {
            System.out.println("Shutting down Graalvisor (hook)...");
            try {
                NativeSandboxInterface.deleteMainCgroup();
            } catch (Exception e) {
                System.out.println("Error deleting main cgroup: " + e.getMessage());
            }
        }
    }
}
