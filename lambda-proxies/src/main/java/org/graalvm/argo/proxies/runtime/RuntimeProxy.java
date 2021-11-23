package org.graalvm.argo.proxies.runtime;

import static org.graalvm.argo.proxies.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.proxies.utils.ProxyUtils.errorResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import org.graalvm.argo.proxies.engine.LanguageEngine;
import org.graalvm.argo.proxies.utils.ProxyUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public abstract class RuntimeProxy {
    public static LanguageEngine languageEngine;
    protected static HttpServer server;
    protected static boolean concurrent;

    public RuntimeProxy(int port, LanguageEngine engine, boolean concurrent) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), -1);
        languageEngine = engine;
        RuntimeProxy.concurrent = concurrent;
    }

    protected void registerInvocationHandler() {
        server.createContext("/", new InvocationHandler());
    }

    protected abstract String invoke(String functionName, String arguments) throws Exception;

    public abstract void start();

    private class InvocationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String jsonBody = ProxyUtils.extractRequestBody(t);
                Map<String, Object> input = jsonToMap(jsonBody);
                String functionName = (String) input.get("name");
                String arguments = (String) input.get("arguments");
                String output = invoke(functionName, arguments);
                ProxyUtils.writeResponse(t, 200, output);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }

}
