package org.graalvm.argo.lambda_proxy;

import java.io.IOException;

import org.graalvm.argo.lambda_proxy.engine.JavaEngine;

public class BaremetalJavaProxy extends JavaProxy {

    /**
     * Entry point of proxies for native java application
     *
     * @param args - expected args are: <timestamp> <target class name> <service port>
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws NumberFormatException
     */
    public static void main(String[] args) throws NumberFormatException, ClassNotFoundException, NoSuchMethodException, IOException {
        checkArgs(args);

        System.out.println("Java Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(args[0])));
        JavaEngine.setFunctionName(args[1]);
        start(new JavaEngine(), Integer.parseInt(args[2]));
    }

}