package org.graalvm.argo.graalvisor;

import java.io.File;

public abstract class Main {

    private static final boolean IS_SVM = System.getProperty("java.vm.name").equals("Substrate VM");
    public static String APP_DIR;

    public static void main(String[] args) throws Exception {
        String lambda_port = System.getenv("lambda_port");
        String lambda_timestamp = System.getenv("lambda_timestamp");
        APP_DIR = System.getenv("app_dir");

        if (lambda_port == null) {
            System.err.println("Error invoking Proxy, service port is null.");
            System.exit(1);
        }

        if (lambda_timestamp == null) {
            System.err.println("Error invoking Proxy, service timestamp is null.");
            System.exit(1);
        }

        if (APP_DIR == null) {
            APP_DIR = "./apps/";
        }

        System.out.println("Polyglot Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(lambda_timestamp)));

        // Create directory where apps will be placed.
        new File(APP_DIR).mkdirs();

        int port = Integer.parseInt(lambda_port);
        (IS_SVM ?  new SubstrateVMProxy(port) : new HotSpotProxy(port)).start();
    }
}
