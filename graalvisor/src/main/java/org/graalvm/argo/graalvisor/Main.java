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
        String lambda_timestamp = System.getenv("lambda_timestamp");

        if (lambda_port == null) {
            System.err.println("Error invoking graalvisor, service port is null.");
            System.exit(1);
        }

        if (lambda_timestamp == null) {
            System.err.println("Error invoking graalvisor, service timestamp is null.");
            System.exit(1);
        }

        if (APP_DIR == null) {
            APP_DIR = "./apps/";
        }

        System.out.println(String.format("Graalvisor boot time: %s ms" ,(System.currentTimeMillis() - Long.parseLong(lambda_timestamp))));

        // Initialize our native sandbox interface.
        NativeSandboxInterface.ginit();

        // Create the directory where function code will be placed.
        new File(APP_DIR).mkdirs();

        int port = Integer.parseInt(lambda_port);

        if (System.getProperty("java.vm.name").equals("Substrate VM")) {
           new SubstrateVMProxy(port).start();
        } else {
           new HotSpotProxy(port).start();
        }
    }
}
