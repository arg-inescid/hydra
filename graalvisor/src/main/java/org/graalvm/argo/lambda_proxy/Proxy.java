package org.graalvm.argo.lambda_proxy;

import java.io.File;
import java.io.IOException;
import org.graalvm.argo.lambda_proxy.engine.LanguageEngine;
import org.graalvm.argo.lambda_proxy.engine.PolyglotEngine;

public abstract class Proxy {

    public static final boolean runInIsolate = System.getProperty("java.vm.name").equals("Substrate VM");

    protected static final String TIMESTAMP_TAG = "lambda_timestamp=";
    protected static final String ENTRY_POINT_TAG = "lambda_entry_point=";
    protected static final String PORT_TAG = "lambda_port=";
    public static final String APP_DIR = "./apps/"; // TODO - move to a variable as well?

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

        // Create directory where apps will be placed.
        new File(APP_DIR).mkdirs();

        LanguageEngine engine = new PolyglotEngine();
        int port = Integer.parseInt(lambda_port);
        RuntimeProxy proxy = runInIsolate ? new SubstrateVMProxy(port, engine, true) : new HotSpotProxy(port, engine, true);
        proxy.start();
    }
}
