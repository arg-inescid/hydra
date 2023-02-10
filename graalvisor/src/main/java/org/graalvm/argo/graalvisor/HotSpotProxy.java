package org.graalvm.argo.graalvisor;

import java.io.IOException;

import org.graalvm.argo.graalvisor.base.PolyglotFunction;

public class HotSpotProxy extends RuntimeProxy {

    public HotSpotProxy(int port) throws IOException {
        super(port);
    }

    @Override
    public String invoke(PolyglotFunction function, boolean cached, String arguments) throws Exception {
        return languageEngine.invoke(function.getName(), arguments);
    }
}
