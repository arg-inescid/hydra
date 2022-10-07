package org.graalvm.argo.lambda_proxy;

import java.io.File;
import java.io.IOException;

import org.graalvm.argo.lambda_proxy.engine.PolyglotEngine;

public class PolyglotProxy extends Proxy {
    public static final String APP_DIR = "./apps/";

    /**
     * @param args - expected args are: <timestamp> <service port>
     * @throws IOException
     * @throws NumberFormatException
     */
    public static void main(String[] args) throws NumberFormatException, IOException {
        String lambda_port = System.getenv("lambda_port");
        String lambda_timestamp = System.getenv("lambda_timestamp");

        if (lambda_port == null) {
            System.err.println("Error invoking Proxy, service port is null.");
            System.exit(1);
        }

        if (lambda_timestamp == null) {
            System.err.println("Error invoking Proxy, service timestamp is null.");
            System.exit(1);
        }

        System.out.println("Polyglot Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(lambda_timestamp)));
        new File(APP_DIR).mkdirs();
        start(new PolyglotEngine(), Integer.parseInt(lambda_port));
    }
}
