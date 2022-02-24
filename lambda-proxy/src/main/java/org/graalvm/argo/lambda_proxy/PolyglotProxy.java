package org.graalvm.argo.lambda_proxy;

import java.io.File;
import java.io.IOException;

import org.graalvm.argo.lambda_proxy.engine.PolyglotEngine;

public class PolyglotProxy extends Proxy {
    public static final String APP_DIR = "./apps/";

    protected static void checkArgs(String[] args) {
        if (args == null || args.length < 2) {
            System.err.println("Error invoking PolyglotProxy, expected at least two arguments (timestamp, service port).");
            System.exit(1);
        }
    }

    /**
     * Entry point of proxies for all truffle functions Difference between JavaProxy and
     * TruffleProxy is that 1. TruffleProxy supports function caching 2. TruffleProxy supports
     * dynamic function loading/registration/removing 3. TruffleProxy exists as deployed native
     * image, running different invocations inside different isolates
     *
     * @param args - expected args are: <timestamp> <service port>
     * @throws IOException
     * @throws NumberFormatException
     */
    public static void main(String[] args) throws NumberFormatException, IOException {
        args = loadArguments(new String[]{TIMESTAMP_TAG, PORT_TAG});
        new File(APP_DIR).mkdirs();
        checkArgs(args);

        System.out.println("Polyglot Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(args[0])));
        start(new PolyglotEngine(), Integer.parseInt(args[1]));
    }

}
