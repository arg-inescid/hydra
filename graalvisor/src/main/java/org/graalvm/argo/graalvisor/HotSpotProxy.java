package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.utils.JsonUtils.json;
import static org.graalvm.argo.graalvisor.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.graalvisor.utils.ProxyUtils.errorResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Executors;

import org.graalvm.argo.graalvisor.function.HotSpotFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;
import org.graalvm.argo.graalvisor.function.TruffleFunction;
import org.graalvm.argo.graalvisor.utils.ProxyUtils;

import com.sun.net.httpserver.HttpExchange;
import com.oracle.svm.graalvisor.polyglot.PolyglotLanguage;
import com.oracle.svm.graalvisor.polyglot.PolyglotEngine;

/**
 * Runtime proxy that runs on HotSpot JVM. Right now we only support truffle
 * code in HotSpot but it can be extended by running Graalvisor runtimes
 * through JNI.
 *
 * The HotSpot proxy serves requests in a sequential manner, i.e., no requests will be
 * executed in parallel.
 */
public class HotSpotProxy extends RuntimeProxy {

    /**
     * Engine that runs truffle functions.
     */
    public static final PolyglotEngine LANGUAGE_ENGINE;

    static {
        PolyglotEngine engine = null;
        try {
            engine = new PolyglotEngine();
        } catch (Throwable e) {
            System.out.println("Warning: graalvisor compiled with no truffle language support.");
        } finally {
            LANGUAGE_ENGINE = engine;
        }
    }

    public HotSpotProxy(int port, String appDir) throws IOException {
        super(port, appDir);
        server.createContext("/agentconfig", new RetrieveAgentConfigHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
    }

    @Override
    public String invoke(PolyglotFunction function, boolean cached, int warmupConc, int warmupReqs, String arguments) throws IOException {
        if (warmupConc != 0 || warmupReqs != 0) {
            return "{'Error': 'Warmup operation in hotspot proxy'}";
        } else if (function.getLanguage() == PolyglotLanguage.JAVA) {
            HotSpotFunction hf = (HotSpotFunction) function;
            Method method = hf.getMethod();
            try {
                return json.asString(method.invoke(null, new Object[] { jsonToMap(arguments) }));
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else if (function instanceof TruffleFunction){
            TruffleFunction tf = (TruffleFunction) function;
            return LANGUAGE_ENGINE.invoke(tf.getLanguage().toString(), tf.getSource(), tf.getEntryPoint(), arguments);
        } else {
            return String.format("{'Error': 'Function %s not registered or not truffle function!'}", function.getName());
        }
    }

    private class RetrieveAgentConfigHandler implements ProxyHttpHandler {

        Map<String, String> configPaths = Map.of("jni",                "config/jni-config.json",
                                                 "predefined-classes", "config/predefined-classes-config.json",
                                                 "proxy",              "config/proxy-config.json",
                                                 "reflect",            "config/reflect-config.json",
                                                 "resource",           "config/resource-config.json",
                                                 "serialization",      "config/serialization-config.json");

        @Override
        public void handleInternal(HttpExchange t) throws IOException {
            try {
                String jsonBody = ProxyUtils.extractRequestBody(t);
                Map<String, Object> input = jsonToMap(jsonBody);
                String configName = (String) input.get("configName");
                String configPath = configPaths.get(configName);
                if (configPath != null) {
                    String configContent = new String(Files.readAllBytes(Paths.get(configPath)));
                    ProxyUtils.writeResponse(t, 200, configContent);
                } else {
                    errorResponse(t, "No such config: " + configName);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }
}
