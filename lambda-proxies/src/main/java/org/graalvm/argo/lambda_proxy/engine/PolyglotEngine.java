package org.graalvm.argo.lambda_proxy.engine;

import static org.graalvm.argo.lambda_proxy.base.TruffleExecutor.*;
import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.lambda_proxy.utils.ProxyUtils.*;

import java.io.IOException;
import java.util.Map;

import org.graalvm.argo.lambda_proxy.base.IsolateObjectWrapper;
import org.graalvm.argo.lambda_proxy.base.PolyglotFunction;
import org.graalvm.argo.lambda_proxy.base.PolyglotLanguage;
import org.graalvm.argo.lambda_proxy.base.TruffleExecutor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class PolyglotEngine implements LanguageEngine {

    @Override
    public String invoke(String functionName, String jsonArguments) {
        if (!functionExists(functionName)) {
            return String.format("{'Error': 'Function %s does not exist!'}", functionName);
        }
        return TruffleExecutor.invoke(functionName, jsonArguments);
    }

    @Override
    public void registerFunction(String functionName, IsolateObjectWrapper targetIsolate) throws Exception {
        if (!functionExists(functionName, targetIsolate)) {
            // register sourceCode into worker isolate
            PolyglotFunction function = getFunction(functionName);
            register(functionName, function, targetIsolate);
        }
    }

    @Override
    public void cleanUp(IsolateObjectWrapper isolateObjectWrapper) {
        TruffleExecutor.deregisterIsolate(isolateObjectWrapper);
    }

    @Override
    public void registerHandler(HttpServer server) {
        server.createContext("/register", new RegisterHandler());
        server.createContext("/deregister", new DeregisterHandler());
    }

    private static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Map<String, Object> input = jsonToMap(extractRequestBody(t));
            String functionName = (String) input.get("name");
            String language = (String) input.get("language");
            String sourceCode = (String) input.get("sourceCode");
            try {
                PolyglotLanguage polyglotLanguage = PolyglotLanguage.valueOf(language.toUpperCase());
                register(functionName, polyglotLanguage.toString(), sourceCode);
                writeResponse(t, 200, String.format("Function %s registered successfully!", input.get("name")));
            } catch (IllegalArgumentException e) {
                e.printStackTrace(System.err);
                errorResponse(t, "Unknown polyglot language: " + language);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }

    private static class DeregisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String functionName = (String) jsonToMap(extractRequestBody(t)).get("name");
                if (!TruffleExecutor.deregister(functionName)) {
                    errorResponse(t, String.format("Function %s has not been registered before!", functionName));
                    return;
                }
                writeResponse(t, 200, String.format("Function %s unregistered!", functionName));
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }
}
