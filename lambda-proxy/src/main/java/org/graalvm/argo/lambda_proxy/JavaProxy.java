package org.graalvm.argo.lambda_proxy;

import java.io.IOException;
import java.util.Arrays;

import org.graalvm.argo.lambda_proxy.engine.JavaEngine;

public class JavaProxy extends Proxy {

    protected static void checkArgs(String[] args) {
        if (args == null || args.length < 3) {
            System.err.println("Error invoking JavaProxy, expected at least three arguments (timestamp, target classname and service port).");
            System.exit(1);
        }
    }

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
        args = loadArguments(new String[]{TIMESTAMP_TAG, ENTRY_POINT_TAG, PORT_TAG});
        checkArgs(args);

        System.out.println("Java Lambda boot time: " + (System.currentTimeMillis() - Long.parseLong(args[0])));
        JavaEngine.setFunctionName(args[1]);
        start(new JavaEngine(), Integer.parseInt(args[2]));
    }

}