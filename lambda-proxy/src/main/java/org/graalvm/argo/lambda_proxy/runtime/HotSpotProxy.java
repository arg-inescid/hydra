package org.graalvm.argo.lambda_proxy.runtime;

import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.graalvm.argo.lambda_proxy.engine.LanguageEngine;

public class HotSpotProxy extends RuntimeProxy {

    public HotSpotProxy(int port, LanguageEngine engine, boolean concurrent) throws IOException {
        super(port, engine, concurrent);
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

    @Override
    public void start() {
        registerInvocationHandler();
        languageEngine.registerHandler(server);
        ExecutorService executorService = concurrent ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(1);
        server.setExecutor(executorService);
        server.start();
    }
}
