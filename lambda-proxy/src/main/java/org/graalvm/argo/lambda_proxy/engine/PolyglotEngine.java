package org.graalvm.argo.lambda_proxy.engine;

import static org.graalvm.argo.lambda_proxy.PolyglotProxy.APP_DIR;
import static org.graalvm.argo.lambda_proxy.utils.IsolateUtils.copyString;
import static org.graalvm.argo.lambda_proxy.utils.IsolateUtils.retrieveString;
import static org.graalvm.argo.lambda_proxy.utils.JsonUtils.jsonToMap;
import static org.graalvm.argo.lambda_proxy.utils.ProxyUtils.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;

import com.oracle.svm.graalvisor.types.GuestIsolateThread;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

// TODO - split into JavaNativeLibraryEngine and TruffleEngine
public class PolyglotEngine implements LanguageEngine {

    /**
     *  FunctionTable is used to store registered functions inside default and worker isolates.
     */
    private static final ConcurrentHashMap<String, PolyglotFunction> functionTable = new ConcurrentHashMap<>();

    /**
     * Each thread owns it truffle context, where polyglot functions execute.
     */
    private Context context;

    /**
     * Each context has a corresponding truffle function that should be used for te invocation.
     */
    private Value function;

    @SuppressWarnings("unused")
    @CEntryPoint
    private static void installSourceCode(@CEntryPoint.IsolateThreadContext IsolateThread workingThread,
                    ObjectHandle functionName,
                    ObjectHandle entryPoint,
                    ObjectHandle language,
                    ObjectHandle sourceCode) {
        functionTable.put(retrieveString(functionName), new PolyglotFunction(retrieveString(functionName), retrieveString(entryPoint), retrieveString(language), retrieveString(sourceCode)));
    }

    @Override
    public String invoke(String functionName, String arguments) throws Exception {
        PolyglotFunction guestFunction = functionTable.get(functionName);
        String resultString = new String();

        if (guestFunction == null) {
            return String.format("{'Error': 'Function %s not registered!'}", functionName);
        }

        if (context == null) {
            Map<String, String> options = new HashMap<>();
            String language = guestFunction.getLanguage().toString();

            if (guestFunction.getLanguage() == PolyglotLanguage.PYTHON) {
                // Allow python imports.
                options.put("python.ForceImportSite", "true");
            }

            // Build context.
            context = Context.newBuilder().allowAllAccess(true).options(options).build();

            // Host access to implement missing language functionalities.
            context.getBindings(language).putMember("polyHostAccess", new PolyglotHostAccess());

            // Evaluate source script to load function into the environment.
            context.eval(language, guestFunction.getSource());

            // Get function handle from the script.
            function = context.eval(language, guestFunction.getEntryPoint());
        }

        try {
            resultString = function.execute(arguments).toString();
        } catch (PolyglotException pe) {
            if (pe.isSyntaxError()) {
                 SourceSection location = pe.getSourceLocation();
                 resultString = String.format("Error happens during parsing/ polyglot function at line %s: %s", location, pe.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error happens during invoking polyglot function: ");
            resultString = String.format("Error happens during invoking polyglot function:: %s", e.getMessage());
        }

        return resultString;
    }

    @Override
    public String invoke(IsolateObjectWrapper workingIsolate, String functionName, String jsonArguments) throws Exception {
        PolyglotFunction guestFunction = functionTable.get(functionName);
        if (guestFunction == null) {
            return String.format("{'Error': 'Function %s not registered!'}", functionName);
        } else if (guestFunction.getLanguage().equals(PolyglotLanguage.JAVA)) {
            GuestIsolateThread guestThread = (GuestIsolateThread) workingIsolate.getIsolateThread();
            return guestFunction.getGraalVisorAPI().invokeFunction(guestThread, guestFunction.getEntryPoint(), jsonArguments);
        } else {
            IsolateThread workingThread = workingIsolate.getIsolateThread();
            return retrieveString(IsolateProxy.invoke(workingThread, CurrentIsolate.getCurrentThread(), copyString(workingThread, functionName), copyString(workingThread, jsonArguments)));
        }
    }

    @Override
    public IsolateObjectWrapper createIsolate(String functionName) {
        PolyglotFunction polyglotFunction = functionTable.get(functionName);
        if (polyglotFunction == null)
            return null;
        else if (polyglotFunction.getLanguage().equals(PolyglotLanguage.JAVA)) {
            GuestIsolateThread guestThread = polyglotFunction.getGraalVisorAPI().createIsolate();
            return new IsolateObjectWrapper(Isolates.getIsolate(guestThread), guestThread);
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
    }

    @Override
    public void tearDownIsolate(String functionName, IsolateObjectWrapper workingIsolate) {
        if (functionName != null && workingIsolate != null && functionTable.containsKey(functionName)) {
            if (functionTable.get(functionName).getLanguage().equals(PolyglotLanguage.JAVA)) {
                functionTable.get(functionName).getGraalVisorAPI().tearDownIsolate((GuestIsolateThread) workingIsolate.getIsolateThread());
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

            long start = System.currentTimeMillis();
            String[] params = t.getRequestURI().getQuery().split("&");
            Map<String, String> metaData = new HashMap<>();
            for (String param : params) {
                String[] keyValue = param.split("=");
                metaData.put(keyValue[0], keyValue[1]);
            }
            // //check
            String functionName = metaData.get("name");
            String soFileName = APP_DIR + functionName;
            synchronized (functionTable) {
                if (functionTable.containsKey(functionName)) {
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
                    functionTable.put(functionName, new PolyglotFunction(functionName, functionEntryPoint, functionLanguage, ""));
                    System.out.println("Loading SO takes: " + (System.nanoTime() - beforeLoad) / 1e6 + "ms");
                } else {
                    try (InputStream sourceInputStream = new BufferedInputStream(t.getRequestBody(), 4096)) {
                        String sourceCode = new String(sourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
                        functionTable.put(functionName, new PolyglotFunction(functionName, functionEntryPoint, functionLanguage, sourceCode));
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
                if (!functionTable.containsKey(functionName)) {
                    errorResponse(t, String.format("Function %s has not been registered before!", functionName));
                    return;
                } else {
                    if (functionTable.get(functionName).getLanguage().equals(PolyglotLanguage.JAVA)) {
                        functionTable.get(functionName).getGraalVisorAPI().close();
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
