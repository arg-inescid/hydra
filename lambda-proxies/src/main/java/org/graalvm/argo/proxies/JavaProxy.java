package org.graalvm.argo.proxies;

import org.graalvm.argo.proxies.engine.JavaEngine;
import org.graalvm.argo.proxies.runtime.HotSpotProxy;
import org.graalvm.argo.proxies.runtime.IsolateProxy;
import org.graalvm.argo.proxies.runtime.RuntimeProxy;

public class JavaProxy {

    public static final boolean runInIsolate = System.getProperty("java.vm.name").equals("Substrate VM");

    /**
     * Entry point of proxies for native java application
     * 
     * @param args - expected args are: <timestamp> <target class name> <service port>
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Error invoking JavaProxy, expected at least three arguments (timestamp, target classname and service port).");
            System.exit(1);
        }
        System.out.println("VMM boot time: " + (System.currentTimeMillis() - Long.parseLong(args[0])));
        try {
            JavaEngine javaEngine = new JavaEngine();
            JavaEngine.setFunctionName(args[1]);
            int port = Integer.parseInt(args[2]);
            RuntimeProxy proxy = runInIsolate ? new IsolateProxy(port, javaEngine, true) : new HotSpotProxy(port, javaEngine, false);
            proxy.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Proxy server can not be started: " + e);
            System.exit(-1);
        }
    }

}