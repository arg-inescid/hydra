package org.graalvm.argo.graalvisor;

import java.io.IOException;

import org.graalvm.argo.graalvisor.base.PolyglotFunction;

/**
 * Runtime proxy that runs on HotSpot JVM. Right now we only support truffle
 * code in HotSpot but it can be extended by running Graalvisor runtimes
 * through JNI.
 */
public class HotSpotProxy extends RuntimeProxy {

    public HotSpotProxy(int port) throws IOException {
        super(port);
    }

    @Override
    public String invoke(PolyglotFunction function, boolean cached, String arguments) throws Exception {
        return RuntimeProxy.LANGUAGE_ENGINE.invoke(function.getName(), arguments);
    }
}
