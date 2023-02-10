package org.graalvm.argo.graalvisor;

import java.io.File;

public abstract class Main {

    public static String APP_DIR = System.getenv("app_dir");

    public static void main(String[] args) throws Exception {
        String lambda_port = System.getenv("lambda_port");
        String lambda_timestamp = System.getenv("lambda_timestamp");
        String lambda_isolation = System.getenv("isolation");

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

        System.out.println("Polyglot Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(lambda_timestamp)));

        // Create directory where apps will be placed.
        new File(APP_DIR).mkdirs();

        int port = Integer.parseInt(lambda_port);

        if (System.getProperty("java.vm.name").equals("Substrate VM")) {
            if (lambda_isolation == null || lambda_isolation.equals("isolate")) {
               new SubstrateVMProxy(port).start();
            } else if (lambda_isolation.equals("process")) {
               // TODO - implement!
            } else {
               System.err.println("Error invoking graalvisor, isolation mode not supported.");
               System.exit(1);
            }
        } else {
           new HotSpotProxy(port).start();
        }
    }
}
