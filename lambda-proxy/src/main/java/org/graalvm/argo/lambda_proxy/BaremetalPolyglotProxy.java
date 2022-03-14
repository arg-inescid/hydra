package org.graalvm.argo.lambda_proxy;

import java.io.IOException;

import org.graalvm.argo.lambda_proxy.engine.PolyglotEngine;

public class BaremetalPolyglotProxy extends PolyglotProxy {

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
        checkArgs(args);

        System.out.println("Polyglot Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(args[0])));
        start(new PolyglotEngine(), Integer.parseInt(args[1]));
    }
}
