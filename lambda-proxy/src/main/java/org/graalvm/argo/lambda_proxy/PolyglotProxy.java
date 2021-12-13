package org.graalvm.argo.lambda_proxy;

import org.graalvm.argo.lambda_proxy.engine.LanguageEngine;
import org.graalvm.argo.lambda_proxy.engine.PolyglotEngine;
import org.graalvm.argo.lambda_proxy.runtime.HotSpotProxy;
import org.graalvm.argo.lambda_proxy.runtime.IsolateProxy;
import org.graalvm.argo.lambda_proxy.runtime.RuntimeProxy;

public class PolyglotProxy {
    private static final boolean runInIsolate = System.getProperty("java.vm.name").equals("Substrate VM");

    /**
     * Entry point of proxies for all truffle functions Difference between JavaProxy and
     * TruffleProxy is that 1. TruffleProxy supports function caching 2. TruffleProxy supports
     * dynamic function loading/registration/removing 3. TruffleProxy exists as deployed native
     * image, running different invocations inside different isolates
     * 
     * @param args - expected args are: <timestamp> <service port>
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Error invoking PolyglotProxy, expected at least two arguments (timestamp, service port).");
            System.exit(1);
        }
        System.out.println("PolyglotProxy VMM boot time: " + (System.currentTimeMillis() - Long.parseLong(args[0])));
        try {
            LanguageEngine truffleEngine = new PolyglotEngine();
            int port = Integer.parseInt(args[1]);
            RuntimeProxy proxy = runInIsolate ? new IsolateProxy(port, truffleEngine, false) : new HotSpotProxy(port, truffleEngine, false);
            proxy.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Proxy server can not be started: " + e);
            System.exit(-1);
        }
    }

}
