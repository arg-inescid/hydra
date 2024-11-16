package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.utils.JsonUtils.json;
import static org.graalvm.argo.graalvisor.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.graalvisor.utils.ProxyUtils.errorResponse;
import static org.graalvm.argo.graalvisor.utils.ProxyUtils.extractRequestBody;
import static org.graalvm.argo.graalvisor.utils.ProxyUtils.writeResponse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.graalvm.argo.graalvisor.function.HotSpotFunction;
import org.graalvm.argo.graalvisor.function.NativeFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;
import org.graalvm.argo.graalvisor.function.TruffleFunction;
import org.graalvm.argo.graalvisor.sandboxing.ContextSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.ContextSnapshotSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.IsolateSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.ExecutableSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.ProcessSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.SandboxProvider;
import org.graalvm.argo.graalvisor.utils.ProxyUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

// TODO - registry should have a type of function code, lib, bin, zip.

/**
 * The runtime proxy exposes a simple webserver that receives three types of requests:
 * - function registration;
 * - function invocation;
 * - function deregistration;
 */
public abstract class RuntimeProxy {

    interface ProxyHttpHandler extends HttpHandler {

        abstract void handleInternal(HttpExchange exchange) throws IOException;

        @Override
        public default void handle(HttpExchange exchange) throws IOException {
            try {
                handleInternal(exchange);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(exchange, "An error has occurred (see logs for details): " + e);
            }
        }
    }

    /**
     *  FunctionTable is used to store registered functions inside default and worker isolates.
     */
    public static final ConcurrentHashMap<String, PolyglotFunction> FTABLE = new ConcurrentHashMap<>();

    /**
     * Simple Http server. It uses a cached thread pool for managing threads.
     */
    protected static HttpServer server;
    /**
     * Location where function code will be placed.
     */
    private static String appDir;

    public RuntimeProxy(int port, String appDir) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), -1);
        server.createContext("/", new InvocationHandler());
        server.createContext("/warmup", new WarmupHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/deregister", new DeregisterHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        RuntimeProxy.appDir = appDir;
    }

    protected abstract String invoke(
            PolyglotFunction functionName,
            boolean cached,
            int warmupConc,
            int warmupReqs,
            String arguments) throws IOException;

   private String invokeWrapper(
            String functionName,
            boolean cached,
            int warmupConc,
            int warmupReqs,
            String arguments) throws IOException {
        PolyglotFunction function = FTABLE.get(functionName);
        String res;

        long start = System.nanoTime();
        if (function == null) {
                res = String.format("{'Error': 'Function %s not registered!'}", functionName);
            } else {
                res = invoke(function, cached, warmupConc, warmupReqs, arguments);
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

    public void stop() {
        server.stop(0);
    }

    private class InvocationHandler implements ProxyHttpHandler {

        @Override
        public void handleInternal(HttpExchange t) throws IOException {
            Map<String, Object> input = jsonToMap(ProxyUtils.extractRequestBody(t));
            String functionName = (String) input.get("name");
            String arguments = (String) input.get("arguments");
            String async = (String)input.get("async");
            boolean cached = input.get("cached") == null ? true : Boolean.parseBoolean((String)input.get("cached"));
            boolean debug = input.get("debug") == null ? false : Boolean.parseBoolean((String)input.get("debug"));

            if (async != null && async.equals("true")) {
                ProxyUtils.writeResponse(t, 200, "Asynchronous request submitted!");
                String output = invokeWrapper(functionName, cached, 0, 0, arguments);
                System.out.println(output);
            } else {
                if (debug) {
                    System.out.println(String.format("Calling %s with arguments %s", functionName, arguments));
                }
                String output = invokeWrapper(functionName, cached, 0, 0, arguments);
                if (debug) {
                    System.out.println(String.format("Calling %s with arguments %s returned ", functionName, arguments, output));
                }
                ProxyUtils.writeResponse(t, 200, output);
            }
        }
    }

    private class WarmupHandler implements ProxyHttpHandler {

        @Override
        public void handleInternal(HttpExchange t) throws IOException {
            Map<String, String> metadata = ProxyUtils.getRequestParameters(t.getRequestURI().getQuery());
            int concurrency = metadata.get("concurrency") == null ? 1 : Integer.parseInt(metadata.get("concurrency"));
            int requests = metadata.get("requests") == null ? 1 : Integer.parseInt(metadata.get("requests"));
            Map<String, Object> input = jsonToMap(ProxyUtils.extractRequestBody(t));
            String functionName = (String) input.get("name");
            String arguments = (String) input.get("arguments");
            String output = invokeWrapper(functionName, false, concurrency, requests, arguments);
            ProxyUtils.writeResponse(t, 200, output);
        }
    }

    private static class RegisterHandler implements ProxyHttpHandler {

        private SandboxProvider getDefaultSandboxProvider(PolyglotFunction function) {
            if (function.isExecutable()) {
                return new ExecutableSandboxProvider(function, appDir);
            } else {
                return new IsolateSandboxProvider(function);
            }
        }

        private SandboxProvider getSandboxProvider(PolyglotFunction function, String sandboxName) {
            if (sandboxName == null) {
                // If no sandbox is provided, get the default one.
                return getDefaultSandboxProvider(function);
            } else if (sandboxName.equals("context")) {
                return new ContextSandboxProvider(function);
            } else if (sandboxName.equals("context-snapshot")) {
                return new ContextSnapshotSandboxProvider(function);
            } else if (sandboxName.equals("isolate")) {
                return new IsolateSandboxProvider(function);
            } else if (sandboxName.equals("process")) {
                return new ProcessSandboxProvider(function);
            } else if (sandboxName.equals("pgo")) {
                return new ExecutableSandboxProvider(function, appDir);
            } else {
                System.err.println(String.format("Invalid sandbox %s for function %s", sandboxName, function.getName()));
                return null;
            }
        }

        @Override
        public void handleInternal(HttpExchange t) throws IOException {
            Map<String, String> metaData = ProxyUtils.getRequestParameters(t.getRequestURI().getQuery());
            String functionName = metaData.get("name");
            String codeFileName = appDir + "/" + functionName;
            String functionEntryPoint = metaData.get("entryPoint");
            String functionLanguage = metaData.get("language");
            String sandboxName = metaData.get("sandbox");
            int svmID = Integer.parseInt(metaData.getOrDefault("svmid", "0"));
            final boolean isExecutable = Boolean.parseBoolean(metaData.get("isBinary"));

            if (System.getProperty("java.vm.name").equals("Substrate VM")) {
                handlePolyglotRegistration(t, functionName, codeFileName, functionEntryPoint, functionLanguage, sandboxName, svmID, isExecutable);
            } else {
                handleHotSpotRegistration(t, functionName, codeFileName, functionEntryPoint);
            }
        }

        private void handleHotSpotRegistration(HttpExchange t, String functionName, String jarFileName, String functionEntryPoint) throws IOException {
            long start = System.currentTimeMillis();
            // This lock will be acquired at most once since only 1 function can be registered in HotSpot mode.
            synchronized (FTABLE) {
                if (!FTABLE.isEmpty()) {
                    writeResponse(t, 200, String.format("Function has already been registered!"));
                    return;
                }

                try (OutputStream fos = new FileOutputStream(jarFileName); InputStream bis = new BufferedInputStream(t.getRequestBody(), 4096)) {
                    // Write JAR to a file.
                    bis.transferTo(fos);

                    // Use class loader explicitly to load new classes from a JAR dynamically.
                    URLClassLoader loader = new URLClassLoader(new URL[] { Paths.get(jarFileName).toUri().toURL() }, this.getClass().getClassLoader());
                    Class<?> cls = Class.forName(functionEntryPoint, true, loader); // TODO - push this into the constructor.
                    Method method = cls.getMethod("main", Map.class);
                    HotSpotFunction function = new HotSpotFunction(functionName, functionEntryPoint, method);
                    FTABLE.put(functionName, function);
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace(System.err);
                    errorResponse(t, "An error has occurred (see logs for details): " + e);
                    return;
                }
            }
            System.out.println(String.format("Function %s registered in %s ms", functionName, (System.currentTimeMillis() - start)));
            writeResponse(t, 200, String.format("Function %s registered!", functionName));
        }

        // TODO - rename to svm function.
        private void handlePolyglotRegistration(HttpExchange t, String functionName, String soFileName, String functionEntryPoint, String functionLanguage, String sandboxName, int svmID, boolean isExecutable) throws IOException {
            long start = System.currentTimeMillis();
            PolyglotFunction function = null;
            SandboxProvider sprovider = null;
            // TODO - avoid large sync block.
            synchronized (FTABLE) {
                if (FTABLE.containsKey(functionName)) {
                    writeResponse(t, 200, String.format("Function %s has already been registered!", functionName));
                    return;
                }

                if (functionLanguage.equalsIgnoreCase("java")) {
                    // TODO - split soFileName by ';', for each path, check if it starts with 'file://'. otherwise wget it.
                    File targetFile = new File(soFileName);
                    if (targetFile.exists() && targetFile.isFile()) {
                        System.out.println(String.format("Reusing %s", soFileName));
                    } else {
                        try (OutputStream fos = new FileOutputStream(soFileName); InputStream bis = new BufferedInputStream(t.getRequestBody(), 4096)) {
                            bis.transferTo(fos);
                        }
                    }
                    function = new NativeFunction(functionName, functionEntryPoint, functionLanguage, soFileName, isExecutable);
                }
                // TODO - this case won't exist any more.
                else {
                    try (InputStream bis = new BufferedInputStream(t.getRequestBody(), 4096)) {
                        String sourceCode = new String(bis.readAllBytes(), StandardCharsets.UTF_8);
                        function = new TruffleFunction(functionName, functionEntryPoint, functionLanguage, sourceCode);
                    }
                }

                sprovider = getSandboxProvider(function, sandboxName);
                if (sprovider == null) {
                    writeResponse(t, 200, String.format("Failed to register function: unknown sandbox %s!", sandboxName));
                    return;
                } else if (sprovider instanceof ContextSnapshotSandboxProvider) {
                    ((ContextSnapshotSandboxProvider) sprovider).setSVMID(svmID);
                }

                function.setSandboxProvider(sprovider);
                sprovider.loadProvider();
                FTABLE.put(functionName, function);
            }
            System.out.println(String.format("Function %s registered with %s sandboxing in %s ms", functionName, sprovider.getName(), (System.currentTimeMillis() - start)));
            writeResponse(t, 200, String.format("Function %s registered!", functionName));
        }
    }

    private static class DeregisterHandler implements ProxyHttpHandler {

        @Override
        public void handleInternal(HttpExchange t) throws IOException {
            String functionName = (String) jsonToMap(extractRequestBody(t)).get("name");
            PolyglotFunction function = FTABLE.get(functionName);
            if (function == null) {
                errorResponse(t, String.format("Function %s has not been registered before!", functionName));
            } else {
                function.getSandboxProvider().unloadProvider();
                FTABLE.remove(functionName);
            }
            writeResponse(t, 200, String.format("Function %s removed!", functionName));
        }
    }
}
