package org.graalvm.argo.lambda_proxy;

import java.io.IOException;

import org.graalvm.argo.lambda_proxy.engine.JavaEngine;

public class JavaProxy extends Proxy {

    /**
     * @param args - expected args are: <timestamp> <target class name> <service port>
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws NumberFormatException
     */
    public static void main(String[] args) throws NumberFormatException, ClassNotFoundException, NoSuchMethodException, IOException {
        String lambda_port = System.getenv("lambda_port");
        String lambda_entrypoint = System.getenv("lambda_entry_point");
        String lambda_timestamp = System.getenv("lambda_timestamp");

        if (lambda_port == null) {
            System.err.println("Error invoking Proxy, service port is null.");
            System.exit(1);
        }

        if (lambda_entrypoint == null) {
            System.err.println("Error invoking Proxy, service lambda_entrypoint is null.");
            System.exit(1);
        }

        if (lambda_timestamp == null) {
            System.err.println("Error invoking Proxy, service timestamp is null.");
            System.exit(1);
        }

        System.out.println("Java Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(lambda_timestamp)));
        JavaEngine.setFunctionName(lambda_entrypoint);
        start(new JavaEngine(), Integer.parseInt(lambda_port));
    }

}
