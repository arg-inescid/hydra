package org.graalvm.argo.lambda_proxy.engine;

import static org.graalvm.argo.lambda_proxy.utils.IsolateUtils.copyString;
import static org.graalvm.argo.lambda_proxy.utils.IsolateUtils.retrieveString;
import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.valueToJson;
import static org.graalvm.argo.lambda_proxy.utils.ProxyUtils.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.argo.lambda_proxy.base.IsolateObjectWrapper;
import org.graalvm.argo.lambda_proxy.base.PolyglotFunction;
import org.graalvm.argo.lambda_proxy.base.PolyglotLanguage;
import org.graalvm.argo.lambda_proxy.runtime.IsolateProxy;
import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class PolyglotEngine implements LanguageEngine {
    // FunctionTable is used to store registered functions inside default and worker isolates
    private static final ConcurrentHashMap<String, PolyglotFunction> functionTable = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    @CEntryPoint
    private static void installSourceCode(@CEntryPoint.IsolateThreadContext IsolateThread workingThread,
                    ObjectHandle functionName,
                    ObjectHandle entryPoint,
                    ObjectHandle language,
                    ObjectHandle sourceCode) {
        functionTable.put(retrieveString(functionName), new PolyglotFunction(retrieveString(functionName), retrieveString(entryPoint), retrieveString(language), retrieveString(sourceCode)));
    }

    public static boolean deregister(String functionName) throws IOException {
        if (!functionTable.containsKey(functionName))
            return false;
        // functionTable.get(functionName).graalVisorAPI.close();
        functionTable.remove(functionName);
        return true;
    }

    @Override
    public String invoke(String functionName, String arguments) {
        PolyglotFunction guestFunction = functionTable.get(functionName);
        if (guestFunction == null) {
            return String.format("{'Error': 'Function %s not registered!'}", functionName);
        } else if (guestFunction.getLanguage().equals(PolyglotLanguage.JAVA)) {
            // call .so to create the isolate
        } else {
            String resultString = "";
            String language = guestFunction.getLanguage().toString();
            String entryPoint = guestFunction.getEntryPoint();
            String sourceCode = guestFunction.getSource();
            try (Context context = Context.newBuilder().allowAllAccess(true).build()) {
                ProxyObject args = ProxyObject.fromMap(jsonToMap(arguments));

                try {
                    // evaluate source script
                    context.eval(language, sourceCode);
                    // get function handle from the script
                    Value function = context.eval(language, entryPoint);
                    Value res = function.execute(args);
                    resultString = valueToJson(res);
                } catch (IllegalArgumentException | IllegalStateException | PolyglotException | UnsupportedOperationException | NullPointerException e) {
                    System.err.println("Error happens during parsing/invoking polyglot function: ");
                    e.printStackTrace();
                    resultString = e.getMessage();
                }
            }
            return resultString;
        }
        return "";
    }

    @Override
    public String invoke(IsolateObjectWrapper workingIsolate, String functionName, String jsonArguments)
                    throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        PolyglotFunction guestFunction = functionTable.get(functionName);
        if (guestFunction == null) {
            return String.format("{'Error': 'Function %s not registered!'}", functionName);
        } else if (guestFunction.getLanguage().equals(PolyglotLanguage.JAVA)) {
            // invoke function using GraalVisorAPI
        } else {
            IsolateThread workingThread = workingIsolate.getIsolateThread();
            return retrieveString(IsolateProxy.invoke(workingThread, CurrentIsolate.getCurrentThread(), copyString(workingThread, functionName), copyString(workingThread, jsonArguments)));
        }
        return "{}";
    }

    @Override
    public IsolateObjectWrapper createIsolate(String functionName) {
        PolyglotFunction polyglotFunction = functionTable.get(functionName);
        if (polyglotFunction == null)
            return null;
        else if (polyglotFunction.getLanguage().equals(PolyglotLanguage.JAVA)) {
            // call .so to create the isolate
        } else {
            // create a new isolate and setup configurations in that isolate.
            IsolateThread isolateThread = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
            Isolate isolate = Isolates.getIsolate(isolateThread);
            // initialize source code into isolate
            installSourceCode(isolateThread,
                            copyString(isolateThread, functionName),
                            copyString(isolateThread, polyglotFunction.getEntryPoint()),
                            copyString(isolateThread, polyglotFunction.getLanguage().name()),
                            copyString(isolateThread, polyglotFunction.getSource()));
            return new IsolateObjectWrapper(isolate, isolateThread);
        }
        return null;
    }

    @Override
    public void tearDownIsolate(String functionName, IsolateObjectWrapper workingIsolate) {
        if (functionName == null || workingIsolate == null || !functionTable.containsKey(functionName)) {
            return;
        } else {
            if (functionTable.get(functionName).getLanguage().equals(PolyglotLanguage.JAVA)) {
                // use .so to tear down
            } else {
                Isolates.tearDownIsolate(workingIsolate.getIsolateThread());
            }
        }
    }

    @Override
    public void registerHandler(HttpServer server) {
        server.createContext("/register", new RegisterHandler());
        server.createContext("/deregister", new DeregisterHandler());
    }

    private static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String[] params = t.getRequestURI().getQuery().split("&");
            Map<String, String> metaData = new HashMap<>();
            for (String param : params) {
                String[] keyValue = param.split("=");
                metaData.put(keyValue[0], keyValue[1]);
            }
            // //check
            String functionName = metaData.get("name");
            if (functionTable.containsKey(functionName)) {
                errorResponse(t, String.format("Function %s has been registered!", functionName));
                return;
            }
            String functionEntryPoint = metaData.get("entryPoint");
            String functionLanguage = metaData.get("language");
            byte[] functionCode = t.getRequestBody().readAllBytes();
            try (FileOutputStream fileOutputStream = new FileOutputStream(functionName)) {
                fileOutputStream.write(functionCode);
            }
            System.out.println("File written");
            if (functionLanguage.equalsIgnoreCase("java")) {

            } else {
                try {
                    String sourceCode = Files.readString(Path.of(functionName));
                    System.out.println(functionName + functionEntryPoint + functionLanguage + sourceCode);
                    functionTable.put(functionName, new PolyglotFunction(functionName, functionEntryPoint, functionLanguage, sourceCode));
                } catch (IOException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Function registered!");
            writeResponse(t, 200, String.format("Function %s registered!", functionName));
        }
    }

    private static class DeregisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String functionName = (String) jsonToMap(extractRequestBody(t)).get("name");
                if (!functionTable.containsKey(functionName)) {
                    errorResponse(t, String.format("Function %s has not been registered before!", functionName));
                    return;
                } else {
                    if (functionTable.get(functionName).getLanguage().equals(PolyglotLanguage.JAVA)) {

                    }
                    functionTable.remove(functionName);
                }
                writeResponse(t, 200, String.format("Function %s removed!", functionName));
            } catch (Exception e) {
                e.printStackTrace(System.err);
                errorResponse(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }
}
