package org.graalvm.argo.lambda_proxy;

import org.graalvm.argo.lambda_proxy.engine.JavaEngine;
import org.graalvm.argo.lambda_proxy.runtime.HotSpotProxy;
import org.graalvm.argo.lambda_proxy.runtime.IsolateProxy;
import org.graalvm.argo.lambda_proxy.runtime.RuntimeProxy;

public class JavaProxy extends Proxy {

    public static final boolean runInIsolate = System.getProperty("java.vm.name").equals("Substrate VM");

    /**
     * Entry point of proxies for native java application
     * 
     * @param args - expected args are: <timestamp> <target class name> <service port>
     */
    public static void main(String[] args) {
        args = loadArguments(new String[] {TIMESTAMP_TAG, ENTRY_POINT_TAG, PORT_TAG});

        if (args == null || args.length < 3) {
            System.err.println("Error invoking JavaProxy, expected at least three arguments (timestamp, target classname and service port).");
            System.exit(1);
        }

        System.out.println("Native Java Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(args[0])));

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