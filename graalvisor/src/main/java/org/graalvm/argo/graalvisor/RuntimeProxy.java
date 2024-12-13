package org.graalvm.argo.graalvisor;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    protected HttpServer server;
    /**
     * Location where function code will be placed.
     */
    private static String appDir;
    /**
     * Number of successfully processed requests. This is a global metric.
     */
    protected static AtomicInteger PROCESSED_REQUESTS = new AtomicInteger(0);

    public RuntimeProxy(int port, String appDir) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), -1);
        server.createContext("/", new InvocationHandler());
        server.createContext("/warmup", new WarmupHandler());
        server.createContext("/stress", new StressHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/deregister", new DeregisterHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        RuntimeProxy.appDir = appDir;
    }

    protected abstract void setMaxSandboxes(PolyglotFunction function, int max);

    protected abstract void invoke(
            HttpExchange he,
            PolyglotFunction functionName,
            boolean cached,
            int warmupConc,
            int warmupReqs,
            long startTime,
            String arguments);

   private void invokeWrapper(
            HttpExchange he,
            String functionName,
            boolean cached,
            int warmupConc,
            int warmupReqs,
            String arguments) {
        PolyglotFunction function = FTABLE.get(functionName);

        if (function == null) {
            String.format("{'Error': 'Function %s not registered!'}", functionName);
        } else {
            invoke(he, function, cached, warmupConc, warmupReqs, System.nanoTime(), arguments);
        }
   }

    protected static void sendReply(HttpExchange he, long startTime, String output) {
        long microLatency = (System.nanoTime() - startTime) / 1000;
        String msg = String.format("{\"result\":\"%s\",\"process time (us)\":%s}", output, microLatency);
        // HttpExchange may be null if the invocation is asynchronous.
        if (he == null) {
            System.out.println(msg);
        } else {
            try {
                HttpUtils.writeResponse(he, 200, msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

            if (async != null && async.equals("true")) {
                invokeWrapper(null, functionName, cached, 0, 0, arguments);
                HttpUtils.writeResponse(t, 200, "Asynchronous request submitted!");
            } else {
                invokeWrapper(t, functionName, cached, 0, 0, arguments);
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
            // TODO - why calling the invokeWrapper? We could directly call the provider to warmup.
            invokeWrapper(t, functionName, false, concurrency, requests, arguments);
        }
    }

    private class StressHandler implements ProxyHttpHandler {

        @Override
        public void handleInternal(HttpExchange t) throws IOException {
            Map<String, String> metadata = HttpUtils.getRequestParameters(t.getRequestURI().getQuery());
            int concurrency = metadata.get("concurrency") == null ? 1 : Integer.parseInt(metadata.get("concurrency"));
            int requests = metadata.get("requests") == null ? 1 : Integer.parseInt(metadata.get("requests"));
            Map<String, Object> input = jsonToMap(HttpUtils.extractRequestBody(t));
            String functionName = (String) input.get("name");
            String arguments = (String) input.get("arguments");
            int currentProcessedRequests = PROCESSED_REQUESTS.intValue();

            PolyglotFunction pf = FTABLE.get(functionName);
            if (pf == null) {
                HttpUtils.writeResponse(t, 200, "Function not registered!");
            }

            System.out.println(String.format("Stress testing with %s threads and %s requests each.", concurrency, requests));

            // Set maximum number of sandboxes for a specific function.
            setMaxSandboxes(pf, concurrency);

            // Submit all function invocations.
            for (int j = 0; j < requests; j++) {
                invokeWrapper(null, functionName, true, 0, 0, arguments);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Reset the value.
            setMaxSandboxes(pf, 0);

            // Wait for all requests to be processed. Note that we just look at the total count,
            // there is no guarantee that these are actually 'our' requests.
            while(PROCESSED_REQUESTS.intValue() < (currentProcessedRequests + requests)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Stress test done!");
            HttpUtils.writeResponse(t, 200, "success!");
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
            // Function language.
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
