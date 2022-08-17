package org.graalvm.argo.lambda_proxy;

import java.io.IOException;
import org.graalvm.argo.lambda_proxy.engine.LanguageEngine;
import org.graalvm.argo.lambda_proxy.runtime.HotSpotProxy;
import org.graalvm.argo.lambda_proxy.runtime.IsolateProxy;
import org.graalvm.argo.lambda_proxy.runtime.RuntimeProxy;

public abstract class Proxy {

    public static final boolean runInIsolate = System.getProperty("java.vm.name").equals("Substrate VM");

    protected static String TIMESTAMP_TAG = "lambda_timestamp=";
    protected static String ENTRY_POINT_TAG = "lambda_entry_point=";
    protected static String PORT_TAG = "lambda_port=";

    public static void start(LanguageEngine engine, int port) throws IOException {
        RuntimeProxy proxy = runInIsolate ? new IsolateProxy(port, engine, true) : new HotSpotProxy(port, engine, true);
        proxy.start();
    }

}
