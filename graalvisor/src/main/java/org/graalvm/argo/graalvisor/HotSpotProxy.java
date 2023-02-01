package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.utils.JsonUtils.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HotSpotProxy extends RuntimeProxy {

    public HotSpotProxy(int port) throws IOException {
        super(port);
    }

    @Override
    public String invoke(String functionName, boolean cached, String arguments) throws Exception {
        long start = System.currentTimeMillis();
        Map<String, Object> output = new HashMap<>();
        String result = languageEngine.invoke(functionName, arguments);
        output.put("result", result);
        output.put("process time", System.currentTimeMillis() - start);
        String ret = null;
        try {
            ret = json.asString(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
