package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.utils.JsonUtils.json;
import static org.graalvm.argo.graalvisor.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.graalvisor.utils.HttpUtils.errorResponse;
import static org.graalvm.argo.graalvisor.utils.HttpUtils.extractRequestBody;
import static org.graalvm.argo.graalvisor.utils.HttpUtils.writeResponse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import org.graalvm.argo.graalvisor.function.HotSpotFunction;
import org.graalvm.argo.graalvisor.function.NativeFunction;
import org.graalvm.argo.graalvisor.function.PolyglotFunction;
import org.graalvm.argo.graalvisor.sandboxing.ContextSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.ContextSnapshotSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.IsolateSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.ExecutableSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.ProcessSandboxProvider;
import org.graalvm.argo.graalvisor.sandboxing.SandboxProvider;
import org.graalvm.argo.graalvisor.utils.HttpUtils;
import org.graalvm.argo.graalvisor.utils.ZipUtils;

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
            Map<String, Object> input = jsonToMap(HttpUtils.extractRequestBody(t));
            String functionName = (String) input.get("name");
            String arguments = (String) input.get("arguments");
            String async = (String)input.get("async");
            boolean cached = input.get("cached") == null ? true : Boolean.parseBoolean((String)input.get("cached"));
            boolean debug = input.get("debug") == null ? false : Boolean.parseBoolean((String)input.get("debug"));

            if (async != null && async.equals("true")) {
                HttpUtils.writeResponse(t, 200, "Asynchronous request submitted!");
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
                HttpUtils.writeResponse(t, 200, output);
            }
        }
    }

    private class WarmupHandler implements ProxyHttpHandler {

        @Override
        public void handleInternal(HttpExchange t) throws IOException {
            Map<String, String> metadata = HttpUtils.getRequestParameters(t.getRequestURI().getQuery());
            int concurrency = metadata.get("concurrency") == null ? 1 : Integer.parseInt(metadata.get("concurrency"));
            int requests = metadata.get("requests") == null ? 1 : Integer.parseInt(metadata.get("requests"));
            Map<String, Object> input = jsonToMap(HttpUtils.extractRequestBody(t));
            String functionName = (String) input.get("name");
            String arguments = (String) input.get("arguments");
            String output = invokeWrapper(functionName, false, concurrency, requests, arguments);
            HttpUtils.writeResponse(t, 200, output);
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

        public PolyglotFunction registerFunction(String functionName, Map<String, String> params) {
            // URL of the function code.
            String functionURL = params.get("url");
            // Path in the local cache (appDir) where we will check if the file exists.
            String functionPath = appDir + "/" + functionURL.substring(functionURL.lastIndexOf('/') + 1);
            // Function entrypoint (used in hotspot mode).
            String functionEntryPoint = params.get("entryPoint");
            // Function language. // TODO - can we remove?
            String functionLanguage = params.get("language");
            // Type of sandbox to use (used in svm mode).
            String sandboxName = params.get("sandbox");
            // SVM sandbox id (used to checkpoint/restore svm sandboxes).
            int svmID = Integer.parseInt(params.getOrDefault("svmid", "0"));
            // Checks if the function code is an executable or a library (used in svm mode).
            final boolean isExecutable = Boolean.parseBoolean(params.get("isBinary"));

            try {
                // Download file if not on the local cache already.
                if (new File(functionPath).length() == 0) {
                    System.out.println(String.format("Downloading %s", functionURL));
                    HttpUtils.downloadFile(functionURL, functionPath);
                    // Note: we rely on file extensions here.
                    if (functionPath.endsWith(".zip")) {
                        ZipUtils.unzip(functionPath, appDir);
                        // Note: this is a convention shared between the function registry and graalvisor.
                        functionPath = functionPath.replace(".zip", ".so");
                    }
                } else {
                    System.out.println(String.format("Reusing %s", functionPath));
                }

                // Register depending on the current vm type.
                return System.getProperty("java.vm.name").equals("Substrate VM") ?
                    handleSvmRegistration(functionName, functionPath, functionEntryPoint, functionLanguage, sandboxName, svmID, isExecutable) :
                    handleHotSpotRegistration(functionName, functionPath, functionEntryPoint);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return null;
            }
         }

        private PolyglotFunction handleHotSpotRegistration(String functionName, String jarFileName, String functionEntryPoint) throws IOException {
            try {
                // Use class loader explicitly to load new classes from a JAR dynamically.
                URLClassLoader loader = new URLClassLoader(new URL[] { Paths.get(jarFileName).toUri().toURL() }, this.getClass().getClassLoader());
                Class<?> cls = Class.forName(functionEntryPoint, true, loader);
                Method method = cls.getMethod("main", Map.class);
                return new HotSpotFunction(functionName, functionEntryPoint, method);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace(System.err);
                return null;
            }
        }

        private PolyglotFunction handleSvmRegistration(String functionName, String soFileName, String functionEntryPoint, String functionLanguage, String sandboxName, int svmID, boolean isExecutable) throws IOException {
            PolyglotFunction function = new NativeFunction(functionName, functionEntryPoint, functionLanguage, soFileName, isExecutable);
            SandboxProvider sprovider = getSandboxProvider(function, sandboxName);
            if (sprovider == null) {
                return null;
            } else if (sprovider instanceof ContextSnapshotSandboxProvider) {
                ((ContextSnapshotSandboxProvider) sprovider).setSVMID(svmID);
            }

            function.setSandboxProvider(sprovider);
            sprovider.loadProvider();
            return function;
        }

        @Override
        public void handleInternal(HttpExchange t) throws IOException {
            // Measure the registration duration.
            long start = System.currentTimeMillis();
            // Extract parameters in the REST request.
            Map<String, String> params = HttpUtils.getRequestParameters(t.getRequestURI().getQuery());
            // Name of the function code file (unique identifier).
            String functionName = params.get("name");

            System.out.println(String.format("Registering function %s: %s", functionName, params));

            if (FTABLE.computeIfAbsent(functionName, n -> registerFunction(functionName, params)) != null) {
                writeResponse(t, 200, String.format("Function %s registered!", functionName));
            } else {
                errorResponse(t, "Failed to register function (see runtime logs for details).");
            }

            System.out.println(String.format("Registering function: %s... done in %s ms!", functionName, (System.currentTimeMillis() - start)));
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
