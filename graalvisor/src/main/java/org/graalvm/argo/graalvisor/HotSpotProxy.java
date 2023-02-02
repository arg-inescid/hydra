package org.graalvm.argo.graalvisor;

import java.io.IOException;

public class HotSpotProxy extends RuntimeProxy {

    public HotSpotProxy(int port) throws IOException {
        super(port);
    }

    @Override
    public String invoke(String functionName, boolean cached, String arguments) throws Exception {
        return languageEngine.invoke(functionName, arguments);
    }
}
