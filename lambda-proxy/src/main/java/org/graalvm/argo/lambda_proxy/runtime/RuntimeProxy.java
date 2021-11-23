package org.graalvm.argo.lambda_proxy.runtime;

import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.lambda_proxy.utils.ProxyUtils.errorResponse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Map;

import org.graalvm.argo.lambda_proxy.base.FunctionRegistrationFailure;
import org.graalvm.argo.lambda_proxy.engine.LanguageEngine;
import org.graalvm.argo.lambda_proxy.utils.ProxyUtils;

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

    protected abstract String invoke(String functionName, String arguments) throws IOException, ClassNotFoundException,
            InvocationTargetException, IllegalAccessException, NoSuchMethodException, FunctionRegistrationFailure;

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
