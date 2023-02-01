package org.graalvm.argo.graalvisor;

import static org.graalvm.argo.graalvisor.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.graalvisor.utils.ProxyUtils.errorResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

import org.graalvm.argo.graalvisor.engine.PolyglotEngine;
import org.graalvm.argo.graalvisor.utils.ProxyUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public abstract class RuntimeProxy {

	public static PolyglotEngine languageEngine;
    protected static HttpServer server;

    public RuntimeProxy(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), -1);
        server.createContext("/", new InvocationHandler());
        server.setExecutor(Proxy.CONCURRENT ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(1));
        languageEngine = new PolyglotEngine();
        languageEngine.registerHandler(server); // TODO - move all handlers to this file.
    }

    protected abstract String invoke(String functionName, boolean cached, String arguments) throws Exception;

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
                    String output = invoke(functionName, cached, arguments);
                    System.out.println(output);
                } else {
                    String output = invoke(functionName, cached, arguments);
                    ProxyUtils.writeResponse(t, 200, output);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }
}
