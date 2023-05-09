package org.graalvm.argo.graalvisor;

import java.io.File;

import org.graalvm.argo.graalvisor.sandboxing.NativeSandboxInterface;

public abstract class Main {

    /**
     * Location where function code will be placed.
     */
    public static String APP_DIR = System.getenv("app_dir");

    public static void main(String[] args) throws Exception {
        String lambda_port = System.getenv("lambda_port");
        String lambda_entry_point = System.getenv("lambda_entry_point");
        String lambda_timestamp = System.getenv("lambda_timestamp");

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

        if (System.getProperty("java.vm.name").equals("Substrate VM")) {
            // Initialize our native sandbox interface.
            NativeSandboxInterface.ginit();
           new SubstrateVMProxy(port).start();
        } else {
           new HotSpotProxy(port, lambda_entry_point).start();
        }
    }
}
