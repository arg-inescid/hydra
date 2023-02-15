package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.Main.APP_DIR;
import static org.graalvm.argo.graalvisor.utils.JsonUtils.json;
import static org.graalvm.argo.graalvisor.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.graalvisor.utils.ProxyUtils.errorResponse;
import static org.graalvm.argo.graalvisor.utils.ProxyUtils.extractRequestBody;
import static org.graalvm.argo.graalvisor.utils.ProxyUtils.writeResponse;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.graalvm.argo.graalvisor.base.NativeFunction;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import org.graalvm.argo.graalvisor.base.PolyglotLanguage;
import org.graalvm.argo.graalvisor.base.TruffleFunction;
import org.graalvm.argo.graalvisor.engine.FunctionStorage;
import org.graalvm.argo.graalvisor.engine.PolyglotEngine;
import org.graalvm.argo.graalvisor.sandboxing.ContextSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.IsolateSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.RuntimeSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.SandboxProvider;
import org.graalvm.argo.graalvisor.utils.ProxyUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * The runtime proxy exposes a simple webserver that receives three types of requests:
 * - function registration;
 * - function invocation;
 * - function deregistration;
 */
public abstract class RuntimeProxy {

   /**
    * Global reference to the engine that runs truffle functions.
    */
   public static final PolyglotEngine LANGUAGE_ENGINE;

   /**
    * Simple Http server. It uses a cached thread pool for managing threads.
    */
    private static HttpServer server;

    public RuntimeProxy(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), -1);
        server.createContext("/", new InvocationHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/deregister", new DeregisterHandler());
        server.setExecutor(Executors.newCachedThreadPool());
    }

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

    protected abstract String invoke(PolyglotFunction functionName, boolean cached, String arguments) throws Exception;

   private String invokeWrapper(String functionName, boolean cached, String arguments) throws Exception {
      PolyglotFunction function = FunctionStorage.FTABLE.get(functionName);
      String res;

      long start = System.nanoTime();
      if (function == null) {
            res = String.format("{'Error': 'Function %s not registered!'}", functionName);
        } else {
            res = invoke(function, cached, arguments);
        }
      long finish = System.nanoTime();

      Map<String, Object> output = new HashMap<>();
        output.put("result", res);
        output.put("process time (us)", (finish - start) / 1000);
        return json.asString(output);
   }

    public void start() {
        server.start();
    }

    private class InvocationHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String jsonBody = ProxyUtils.extractRequestBody(t);
                Map<String, Object> input = jsonToMap(jsonBody);
                String functionName = (String) input.get("name");
                String arguments = (String) input.get("arguments");
                String async = (String)input.get("async");
                boolean cached = input.get("cached") == null ? true : Boolean.parseBoolean((String)input.get("cached"));

                if (async != null && async.equals("true")) {
                    ProxyUtils.writeResponse(t, 200, "Asynchronous request submitted!");
                    String output = invokeWrapper(functionName, cached, arguments);
                    System.out.println(output);
                } else {
                    String output = invokeWrapper(functionName, cached, arguments);
                    ProxyUtils.writeResponse(t, 200, output);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }

    private static class RegisterHandler implements HttpHandler {

        private SandboxProvider getDefaultSandboxProvider(PolyglotFunction function) {
            if (function.getLanguage() == PolyglotLanguage.JAVA) {
                return new RuntimeSandboxProvider(function);
            } else {
                return new ContextSandboxProvider(function);
            }
        }

        private SandboxProvider getSandboxProvider(PolyglotFunction function, String sandboxName) {

            if (sandboxName == null) {
                return getDefaultSandboxProvider(function);
            }

            if (function.getLanguage() == PolyglotLanguage.JAVA) {
                if (sandboxName.equals("isolate")) {
                    return new IsolateSandboxProvider(function);
                } else if (sandboxName.equals("runtime")) {
                    return new RuntimeSandboxProvider(function);
                }
            } else {
                if (sandboxName.equals("context")) {
                    return new ContextSandboxProvider(function);
                }
            }

            return null;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {

            long start = System.currentTimeMillis();
            String[] params = t.getRequestURI().getQuery().split("&");
            Map<String, String> metaData = new HashMap<>();
            for (String param : params) {
                String[] keyValue = param.split("=");
                metaData.put(keyValue[0], keyValue[1]);
            }
            String functionName = metaData.get("name");
            String soFileName = APP_DIR + functionName;
            String functionEntryPoint = metaData.get("entryPoint");
            String functionLanguage = metaData.get("language");
            String sandboxName = metaData.get("sandbox");
            PolyglotFunction function = null;
            SandboxProvider sprovider = null;

            synchronized (FunctionStorage.FTABLE) {
                if (FunctionStorage.FTABLE.containsKey(functionName)) {
                    writeResponse(t, 200, String.format("Function %s has already been registered!", functionName));
                    return;
                }

                if (functionLanguage.equalsIgnoreCase("java")) {
                    try (OutputStream fos = new FileOutputStream(soFileName); InputStream bis = new BufferedInputStream(t.getRequestBody(), 4096)) {
                        bis.transferTo(fos);
                    }
                    function = new NativeFunction(functionName, functionEntryPoint, functionLanguage, APP_DIR + functionName);
                } else {
                    try (InputStream bis = new BufferedInputStream(t.getRequestBody(), 4096)) {
                        String sourceCode = new String(bis.readAllBytes(), StandardCharsets.UTF_8);
                        function = new TruffleFunction(functionName, functionEntryPoint, functionLanguage, sourceCode);
                    }
                }
                sprovider = getSandboxProvider(function, sandboxName);

                if (sprovider == null) {
                    writeResponse(t, 200, String.format("Failed to register fuction: unknown sandbox %s!", sandboxName));
                    return;
                }

                function.setSandboxProvider(sprovider);
                sprovider.loadProvider();
                FunctionStorage.FTABLE.put(functionName, function);
            }

            System.out.println(String.format("Function %s registered with %s sandboxing in %s ms", functionName, sprovider.getName(), (System.currentTimeMillis() - start)));
            writeResponse(t, 200, String.format("Function %s registered!", functionName));
        }
    }

    private static class DeregisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String functionName = (String) jsonToMap(extractRequestBody(t)).get("name");
                PolyglotFunction function = FunctionStorage.FTABLE.get(functionName);
                if (function == null) {
                    errorResponse(t, String.format("Function %s has not been registered before!", functionName));
                } else {
                   function.getSandboxProvider().unloadProvider();
                    FunctionStorage.FTABLE.remove(functionName);
                }
                writeResponse(t, 200, String.format("Function %s removed!", functionName));
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }
}
