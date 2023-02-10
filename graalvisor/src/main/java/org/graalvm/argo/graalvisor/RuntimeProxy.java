package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.Main.APP_DIR;
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
import org.graalvm.argo.graalvisor.utils.ProxyUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public abstract class RuntimeProxy {

	protected static PolyglotEngine languageEngine;
    private static HttpServer server;

    public RuntimeProxy(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), -1);
        server.createContext("/", new InvocationHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/deregister", new DeregisterHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        languageEngine = new PolyglotEngine();
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
            synchronized (FunctionStorage.FTABLE) {
                if (FunctionStorage.FTABLE.containsKey(functionName)) {
                    writeResponse(t, 200, String.format("Function %s has already been registered!", functionName));
                    return;
                }
                String functionEntryPoint = metaData.get("entryPoint");
                String functionLanguage = metaData.get("language");

                if (functionLanguage.equalsIgnoreCase("java")) {
                    if (!new File(soFileName).exists()) {
                        try (FileOutputStream fileOutputStream = new FileOutputStream(soFileName);
                                        InputStream sourceInputStream = new BufferedInputStream(t.getRequestBody(), 4096)) {
                            sourceInputStream.transferTo(fileOutputStream);
                        }
                    }
                    long beforeLoad = System.nanoTime();
                    FunctionStorage.FTABLE.put(functionName, new NativeFunction(functionName, functionEntryPoint, functionLanguage));
                    System.out.println("Loading SO takes: " + (System.nanoTime() - beforeLoad) / 1e6 + "ms");
                } else {
                    try (InputStream sourceInputStream = new BufferedInputStream(t.getRequestBody(), 4096)) {
                        String sourceCode = new String(sourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
                        FunctionStorage.FTABLE.put(functionName, new TruffleFunction(functionName, functionEntryPoint, functionLanguage, sourceCode));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Function registered! time took: " + (System.currentTimeMillis() - start) + "ms");
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
                    if (function.getLanguage().equals(PolyglotLanguage.JAVA)) {
                        ((NativeFunction)function).getGraalVisorAPI().close();
                    }
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
